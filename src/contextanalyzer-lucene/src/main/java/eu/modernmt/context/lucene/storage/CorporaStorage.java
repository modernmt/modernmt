package eu.modernmt.context.lucene.storage;

import eu.modernmt.context.lucene.analysis.ContextAnalyzerIndex;
import eu.modernmt.data.DataBatch;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by davide on 22/09/16.
 */
public class CorporaStorage {

    private static final int MAX_CONCURRENT_BUCKET_ANALYSIS = 8;

    private final Logger logger = LogManager.getLogger(CorporaStorage.class);

    private final Options options;
    private final AnalysisTimer analysisTimer;
    private final ExecutorService analysisExecutor;

    private final ContextAnalyzerIndex contextAnalyzer;
    private final CorporaIndex index;
    private final Set<LanguagePair> languages;
    private HashSet<CorpusBucket> pendingUpdatesBuckets = new HashSet<>();

    public CorporaStorage(File path, Options options, ContextAnalyzerIndex contextAnalyzer, Collection<LanguagePair> languages) throws IOException {
        this.languages = new HashSet<>(languages);
        this.analysisExecutor = Executors.newFixedThreadPool(options.analysisThreads);

        this.options = options;
        this.contextAnalyzer = contextAnalyzer;

        FileUtils.forceMkdir(path);

        File indexPath = new File(path, "index");

        if (indexPath.exists())
            this.index = CorporaIndex.load(options.analysisOptions, indexPath, path);
        else
            this.index = new CorporaIndex(indexPath, options.analysisOptions, path);

        this.analyzeIfNeeded(this.index.getBuckets());

        this.analysisTimer = new AnalysisTimer();
        this.analysisTimer.start();
    }

    public CorpusBucket getBucket(long memory, LanguagePair direction) throws IOException {
        return this.index.getBucket(direction, memory, false);
    }

    public int size() {
        return index.getBuckets().size();
    }

    public synchronized Collection<Deletion> onDataReceived(DataBatch batch) throws IOException {
        List<Deletion> deletions = new ArrayList<>(batch.getDeletions().size());

        for (TranslationUnit unit : batch.getTranslationUnits()) {
            if (!index.shouldAcceptData(unit.channel, unit.channelPosition))
                continue;

            if (languages.contains(unit.direction)) {
                CorpusBucket bucket = index.getBucket(unit.direction, unit.memory);
                bucket.append(unit.rawSentence);
                pendingUpdatesBuckets.add(bucket);
            }

            if (languages.contains(unit.direction.reversed())) {
                CorpusBucket bucket = index.getBucket(unit.direction.reversed(), unit.memory);
                bucket.append(unit.rawTranslation);
                pendingUpdatesBuckets.add(bucket);
            }
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

    public void bulkInsert(long memory, MultilingualCorpus corpus) throws IOException {
        MultilingualCorpus.MultilingualLineReader reader = null;

        try {
            reader = corpus.getContentReader();

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                if (languages.contains(pair.language)) {
                    CorpusBucket bucket = index.getBucket(pair.language, memory);
                    bucket.append(pair.source);
                    pendingUpdatesBuckets.add(bucket);
                }

                if (languages.contains(pair.language.reversed())) {
                    CorpusBucket bucket = index.getBucket(pair.language.reversed(), memory);
                    bucket.append(pair.target);
                    pendingUpdatesBuckets.add(bucket);
                }
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

        logger.info("Bulk insert of memory " + memory);
    }

    private void analyzeIfNeeded(Collection<CorpusBucket> buckets) throws IOException {
        List<CorpusBucket> filteredBuckets = buckets.stream()
                .filter(CorpusBucket::shouldAnalyze)
                .collect(Collectors.toList());

        if (filteredBuckets.isEmpty()) {
            filteredBuckets = buckets.stream()
                    .filter(CorpusBucket::hasUnanalyzedContent)
                    .collect(Collectors.toList());

            if (filteredBuckets.size() > MAX_CONCURRENT_BUCKET_ANALYSIS)
                filteredBuckets = filteredBuckets.subList(0, MAX_CONCURRENT_BUCKET_ANALYSIS);
        }

        this.doAnalyze(filteredBuckets);
    }

    private void doAnalyze(Collection<CorpusBucket> buckets) throws IOException {
        if (buckets.isEmpty())
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
                Throwable cause = e.getCause();

                if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                else if (cause instanceof IOException)
                    throw (IOException) cause;
                else
                    throw new Error("Unexpected exception", cause);
            }
        }

        this.contextAnalyzer.flush();
        this.contextAnalyzer.invalidateCache();
    }

    public void shutdown() {
        analysisTimer.shutdown();
        analysisExecutor.shutdownNow();
    }

    public void awaitTermination(TimeUnit unit, long timeout) throws InterruptedException {
        Thread waitThread = new Thread(() -> {
            try {
                analysisTimer.join();
            } catch (InterruptedException e) {
                // Ignore it
            }

            try {
                analysisExecutor.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                // Ignore it
            }
        });
        waitThread.start();

        unit.timedJoin(waitThread, timeout);
    }

    private class AnalysisTimer extends Thread {

        private final SynchronousQueue<Object> shutdownSignal = new SynchronousQueue<>();
        private boolean shuttingDown = false;

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

        @Override
        public void run() {
            while (true) {
                Object poisonPill;

                try {
                    poisonPill = shutdownSignal.poll(options.writeBehindDelay, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    break;
                }

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
}
