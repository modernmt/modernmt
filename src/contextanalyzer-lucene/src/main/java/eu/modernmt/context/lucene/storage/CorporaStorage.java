package eu.modernmt.context.lucene.storage;

import eu.modernmt.context.lucene.analysis.ContextAnalyzerIndex;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataMessage;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguageIndex;
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
public class CorporaStorage implements DataListener {

    private static final int MAX_CONCURRENT_BUCKET_ANALYSIS = 8;

    private final Logger logger = LogManager.getLogger(CorporaStorage.class);

    private final Options options;
    private final BackgroundTask backgroundTask;
    private final ExecutorService analysisExecutor;

    private final ContextAnalyzerIndex contextAnalyzer;
    private final CorporaIndex index;
    private final LanguageIndex languages;
    private HashSet<CorpusBucket> pendingUpdatesBuckets = new HashSet<>();

    public CorporaStorage(File path, Options options, ContextAnalyzerIndex contextAnalyzer, LanguageIndex languages) throws IOException {
        this.languages = languages;
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

        this.backgroundTask = new BackgroundTask(options.queueSize);
        this.backgroundTask.start();
    }

    public CorpusBucket getBucket(long domain, LanguagePair direction) throws IOException {
        return this.index.getBucket(direction, domain, false);
    }

    public int size() {
        return index.getBuckets().size();
    }

    @Override
    public void onDataReceived(List<TranslationUnit> batch) throws IOException {
        for (TranslationUnit unit : batch) {
            try {
                backgroundTask.enqueue(unit);
            } catch (InterruptedException e) {
                throw new IOException("Operation interrupted", e);
            }
        }
    }

    @Override
    public void onDelete(Deletion deletion) throws IOException {
        try {
            backgroundTask.enqueue(deletion);
        } catch (InterruptedException e) {
            throw new IOException("Operation interrupted", e);
        }
    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return index.getChannels();
    }

    public synchronized void flushToDisk(boolean skipAnalysis, boolean forceAnalysis) throws IOException {
        if (pendingUpdatesBuckets.isEmpty())
            return;

        logger.info("Flushing index to disk. Pending updates: " + pendingUpdatesBuckets.size());

        for (CorpusBucket bucket : pendingUpdatesBuckets) {
            if (bucket.isDeleted()) {
                bucket.delete();
                index.remove(bucket);
            } else {
                bucket.flush();
            }
        }

        if (!skipAnalysis || forceAnalysis) {
            if (forceAnalysis)
                doAnalyze(pendingUpdatesBuckets);
            else
                analyzeIfNeeded(pendingUpdatesBuckets);
        }

        pendingUpdatesBuckets.clear();
        index.save();

        logger.debug("CorporaStorage index successfully written to disk");
    }

    public void bulkInsert(long domain, MultilingualCorpus corpus) throws IOException {
        MultilingualCorpus.MultilingualLineReader reader = null;

        HashMap<LanguagePair, CorpusBucket> buckets = new HashMap<>();

        try {
            reader = corpus.getContentReader();

            MultilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null) {
                LanguagePair language = languages.map(pair.language);

                if (language == null)
                    continue;

                if (languages.isSupported(language)) {
                    CorpusBucket bucket = buckets.computeIfAbsent(language, direction -> {
                        try {
                            return index.getBucket(direction, domain);
                        } catch (IOException e) {
                            throw new RuntimeIOException(e);
                        }
                    });

                    bucket.append(pair.source);
                }

                language = language.reversed();

                if (languages.isSupported(language)) {
                    CorpusBucket bucket = buckets.computeIfAbsent(language, direction -> {
                        try {
                            return index.getBucket(direction, domain);
                        } catch (IOException e) {
                            throw new RuntimeIOException(e);
                        }
                    });

                    bucket.append(pair.target);
                }
            }

            pendingUpdatesBuckets.addAll(buckets.values());
        } catch (RuntimeIOException e) {
            throw e.cause;
        } finally {
            IOUtils.closeQuietly(reader);
        }

        logger.info("Bulk insert of domain " + domain);
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
        backgroundTask.shutdown();
        analysisExecutor.shutdownNow();
    }

    public void awaitTermination(TimeUnit unit, long timeout) throws InterruptedException {
        Thread waitThread = new Thread(() -> {
            try {
                backgroundTask.join();
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

    private static final DataMessage POISON_PILL = new DataMessage((short) 0, 0) {
    };

    private class BackgroundTask extends Thread {

        private final BlockingQueue<DataMessage> queue;
        private boolean shuttingDown = false;
        private IOException error = null;

        private long lastWriteDate = 0;

        public BackgroundTask(int queueSize) {
            this.queue = new ArrayBlockingQueue<>(queueSize);
        }

        public void enqueue(DataMessage message) throws InterruptedException, IOException {
            if (error != null)
                throw error;

            if (!shuttingDown)
                queue.put(message);
        }

        public void shutdown() {
            if (!shuttingDown) {
                shuttingDown = true;
                queue.clear();

                while (!queue.add(POISON_PILL)) ;
            }
        }

        private DataMessage poll(long timeout) {
            if (timeout < 1)
                return null;

            try {
                return queue.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return POISON_PILL;
            }
        }

        private void doRun() throws IOException {
            while (true) {
                long availableTime = options.writeBehindDelay - (System.currentTimeMillis() - lastWriteDate);

                DataMessage message = poll(availableTime);

                if (message == null) {
                    // timeout
                    flushToDisk(false, false);
                    lastWriteDate = System.currentTimeMillis();
                } else if (message == POISON_PILL) {
                    logger.debug("CorporaStorage background thread KILL");
                    break;
                } else if (index.registerData(message.channel, message.channelPosition)) {
                    if (message instanceof TranslationUnit) {
                        TranslationUnit unit = (TranslationUnit) message;

                        if (languages.isSupported(unit.direction)) {
                            CorpusBucket bucket = index.getBucket(unit.direction, unit.domain);
                            bucket.append(unit.rawSourceSentence);
                            pendingUpdatesBuckets.add(bucket);
                        }

                        if (languages.isSupported(unit.direction.reversed())) {
                            CorpusBucket bucket = index.getBucket(unit.direction.reversed(), unit.domain);
                            bucket.append(unit.rawTargetSentence);
                            pendingUpdatesBuckets.add(bucket);
                        }
                    } else if (message instanceof Deletion) {
                        Deletion deletion = (Deletion) message;

                        for (CorpusBucket bucket : index.getBucketsByDomain(deletion.domain)) {
                            bucket.markForDeletion();
                            pendingUpdatesBuckets.add(bucket);
                        }
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                doRun();
                flushToDisk(true, false);
            } catch (IOException e) {
                error = e;
            } finally {
                IOUtils.closeQuietly(index);
            }
        }

    }
}
