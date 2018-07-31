package eu.modernmt.context.lucene.storage;

import eu.modernmt.context.lucene.analysis.ContextAnalyzerIndex;
import eu.modernmt.context.lucene.analysis.DocumentBuilder;
import eu.modernmt.data.DataBatch;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by davide on 22/09/16.
 */
public class CorporaStorage implements Closeable {

    private final Logger logger = LogManager.getLogger(CorporaStorage.class);

    protected final File path;
    private final Options options;
    private final AnalysisTimer analysisTimer;
    private final ExecutorService analysisExecutor;

    private final ContextAnalyzerIndex contextAnalyzer;
    private final CorporaIndex index;
    private HashSet<CorpusBucket> pendingUpdatesBuckets = new HashSet<>();

    public CorporaStorage(File path, Options options, ContextAnalyzerIndex contextAnalyzer) throws IOException {
        this.analysisExecutor = Executors.newFixedThreadPool(options.analysisThreads);

        this.options = options;
        this.contextAnalyzer = contextAnalyzer;
        this.path = path;

        FileUtils.forceMkdir(path);

        File indexPath = new File(path, "index");

        if (indexPath.exists())
            this.index = CorporaIndex.load(options, indexPath, path);
        else
            this.index = new CorporaIndex(indexPath, options, path);

        this.analyzeIfNeeded(this.index.getBuckets());

        if (options.enableAnalysis) {
            this.analysisTimer = new AnalysisTimer();
            this.analysisTimer.start();
        } else {
            this.analysisTimer = null;
        }
    }

    public CorpusBucket getBucket(String docId) throws IOException {
        // createIfAbsent == false, so owner can only be read and not created (null is acceptable)
        return this.index.getBucket(null, docId, false);
    }

    public int size() {
        return index.getBuckets().size();
    }

    public synchronized Collection<Deletion> onDataReceived(DataBatch batch) throws IOException {
        List<Deletion> deletions = new ArrayList<>(batch.getDeletions().size());

        for (TranslationUnit unit : batch.getTranslationUnits()) {
            if (!index.shouldAcceptData(unit.channel, unit.channelPosition))
                continue;

            CorpusBucket fwdBucket = index.getBucket(unit.owner, DocumentBuilder.makeId(unit.memory, unit.direction));
            fwdBucket.append(unit.rawSentence);
            pendingUpdatesBuckets.add(fwdBucket);

            CorpusBucket bwdBucket = index.getBucket(unit.owner, DocumentBuilder.makeId(unit.memory, unit.direction.reversed()));
            bwdBucket.append(unit.rawTranslation);
            pendingUpdatesBuckets.add(bwdBucket);
        }

        for (Deletion deletion : batch.getDeletions()) {
            if (!index.shouldAcceptData(deletion.channel, deletion.channelPosition))
                continue;

            deletions.add(deletion);

            for (CorpusBucket bucket : index.getBucketsByMemory(deletion.memory)) {
                bucket.markForDeletion();
                pendingUpdatesBuckets.add(bucket);
            }
        }

        index.advanceChannels(batch.getChannelPositions());

        return deletions;
    }

    public Map<Short, Long> getLatestChannelPositions() {
        return index.getChannels();
    }

    public synchronized void flushToDisk(boolean skipAnalysis, boolean forceAnalysis) throws IOException {
        if (pendingUpdatesBuckets.isEmpty())
            return;

        logger.info("Flushing index to disk. Pending updates: " + pendingUpdatesBuckets.size());

        for (Iterator<CorpusBucket> iterator = pendingUpdatesBuckets.iterator(); iterator.hasNext(); ) {
            CorpusBucket bucket = iterator.next();

            if (bucket.isDeleted()) {
                bucket.delete();
                index.remove(bucket);

                iterator.remove();
            } else {
                bucket.flush();
            }
        }

        if (!skipAnalysis || forceAnalysis) {
            if (forceAnalysis)
                doAnalyze(pendingUpdatesBuckets);
            else
                analyzeIfNeeded(pendingUpdatesBuckets);

            pendingUpdatesBuckets.clear();
        }

        index.save();

        logger.debug("CorporaStorage index successfully written to disk");
    }

    private void analyzeIfNeeded(Collection<CorpusBucket> buckets) throws IOException {
        List<CorpusBucket> filteredBuckets = buckets.stream()
                .filter(CorpusBucket::shouldAnalyze)
                .collect(Collectors.toList());

        if (filteredBuckets.isEmpty()) {
            filteredBuckets = buckets.stream()
                    .filter(CorpusBucket::hasUnanalyzedContent)
                    .collect(Collectors.toList());

            if (filteredBuckets.size() > options.maxConcurrentAnalyses)
                filteredBuckets = filteredBuckets.subList(0, options.maxConcurrentAnalyses);
        }

        this.doAnalyze(filteredBuckets);
    }

    private void doAnalyze(Collection<CorpusBucket> buckets) throws IOException {
        if (!options.enableAnalysis || buckets.isEmpty())
            return;

        ArrayList<Future<Void>> pendingAnalysis = new ArrayList<>(buckets.size());

        for (CorpusBucket bucket : buckets) {
            if (bucket.isDeleted())
                continue;

            AnalysisTask task = new AnalysisTask(contextAnalyzer, bucket);
            try {
                pendingAnalysis.add(analysisExecutor.submit(task));
            } catch (RejectedExecutionException e) {
                // Shutting down, ignore analyze instruction
                return;
            }
        }

        for (Future<Void> analysis : pendingAnalysis) {
            try {
                analysis.get();
            } catch (InterruptedException e) {
                throw new IOException("Analysis has been interrupted", e);
            } catch (ExecutionException e) {
                throw unpack(e);
            }
        }

        this.contextAnalyzer.flush();
        this.contextAnalyzer.invalidateCache();
    }

    public void compress() throws IOException {
        this.flushToDisk(false, true);

        int threads = Math.max(10, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        try {
            if (this.analysisTimer != null)
                this.analysisTimer.setEnabled(false);
            ExecutorCompletionService<CorpusBucket.Compression> ecs = new ExecutorCompletionService<>(executor);

            int size = 0;
            for (final CorpusBucket bucket : index.getBuckets()) {
                if (bucket.hasUncompressedContent()) {
                    ecs.submit(bucket::compress);
                    size++;
                }
            }

            for (int i = 0; i < size; i++) {
                CorpusBucket.Compression compression = ecs.take().get();

                this.index.save();

                try {
                    compression.commit();
                    logger.info("(" + (i + 1) + "/" + size + ") Compression completed for bucket " + compression.getBucket());
                } catch (IOException e) {
                    logger.warn("Failed to compress bucket " + compression.getBucket() + ", rolling back", e);
                    compression.rollback();
                    this.index.save();
                }
            }
        } catch (InterruptedException e) {
            throw new IOException("Interrupted execution", e);
        } catch (ExecutionException e) {
            throw unpack(e);
        } finally {
            if (this.analysisTimer != null)
                this.analysisTimer.setEnabled(true);

            executor.shutdownNow();
        }
    }

    @Override
    public void close() {
        if (this.analysisTimer != null) {
            try {
                analysisTimer.shutdown();
                analysisTimer.join();
            } catch (InterruptedException e) {
                // Ignore it
            }
        }

        try {
            analysisExecutor.shutdownNow();
            analysisExecutor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignore it
        }
    }

    private class AnalysisTimer extends Thread {

        private final SynchronousQueue<Object> shutdownSignal = new SynchronousQueue<>();
        private boolean shuttingDown = false;
        private boolean enabled = true;

        public void shutdown() {
            if (!shuttingDown) {
                shuttingDown = true;

                try {
                    shutdownSignal.put(new Object());
                } catch (InterruptedException e) {
                    this.interrupt();
                }
            }
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void run() {
            while (true) {
                Object poisonPill;

                try {
                    poisonPill = shutdownSignal.poll(options.writeBehindDelay, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    break;
                }

                if (!enabled)
                    continue;

                if (poisonPill == null) { // timeout
                    try {
                        flushToDisk(false, false);
                    } catch (IOException e) {
                        logger.error("Failed to flush CorporaStorage to disk", e);
                    }
                } else { // poison pill
                    break;
                }
            }

            try {
                flushToDisk(true, false);
            } catch (IOException e) {
                logger.error("Failed to flush CorporaStorage to disk", e);
            } finally {
                IOUtils.closeQuietly(index);
            }
        }

    }

    private static IOException unpack(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException)
            throw (RuntimeException) cause;
        else if (cause instanceof IOException)
            return (IOException) cause;
        else
            throw new Error("Unexpected exception", cause);
    }

}
