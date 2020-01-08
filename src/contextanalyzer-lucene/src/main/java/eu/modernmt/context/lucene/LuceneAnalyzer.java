package eu.modernmt.context.lucene;

import eu.modernmt.config.AnalyzerConfig;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.lucene.analysis.ContextAnalyzerIndex;
import eu.modernmt.context.lucene.analysis.DocumentBuilder;
import eu.modernmt.context.lucene.storage.Bucket;
import eu.modernmt.context.lucene.storage.CorporaStorage;
import eu.modernmt.data.LogDataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.impl.StringCorpus;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.document.Document;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by davide on 09/05/16.
 */
public class LuceneAnalyzer implements ContextAnalyzer, DataListenerProvider {

    private final Logger logger = LogManager.getLogger(LuceneAnalyzer.class);

    private final ContextAnalyzerIndex index;
    private final CorporaStorage storage;
    private final AnalysisThread analysis;

    public LuceneAnalyzer(File indexPath, AnalyzerConfig config) throws IOException {
        this(new ContextAnalyzerIndex(new File(indexPath, "index")), new CorporaStorage(new File(indexPath, "storage")), config);
    }

    protected LuceneAnalyzer(ContextAnalyzerIndex index, CorporaStorage storage, AnalyzerConfig config) {
        this.index = index;
        this.storage = storage;

        if (config.analyze()) {
            this.analysis = new AnalysisThread(config);
            this.analysis.start();
        } else {
            this.analysis = null;
        }
    }

    public ContextAnalyzerIndex getIndex() {
        return index;
    }

    public CorporaStorage getStorage() {
        return storage;
    }

    @Override
    public ContextVector getContextVector(UUID user, LanguageDirection direction, String query, int limit) throws ContextAnalyzerException {
        return getContextVector(user, direction, new StringCorpus(null, direction.source, query), limit);
    }

    @Override
    public ContextVector getContextVector(UUID user, LanguageDirection direction, File source, int limit) throws ContextAnalyzerException {
        return getContextVector(user, direction, new FileCorpus(source, null, direction.source), limit);
    }

    @Override
    public ContextVector getContextVector(UUID user, LanguageDirection direction, Corpus query, int limit) throws ContextAnalyzerException {
        try {
            return this.index.getContextVector(user, direction, query, limit);
        } catch (IOException e) {
            throw new ContextAnalyzerException("Failed to calculate context-vector due an internal error", e);
        }
    }

    @Override
    public synchronized void optimize() throws ContextAnalyzerException {
        logger.info("Starting memory forced merge");
        long begin = System.currentTimeMillis();
        try {
            this.index.forceMerge();
        } catch (IOException e) {
            throw new ContextAnalyzerException(e);
        }
        long elapsed = System.currentTimeMillis() - begin;
        logger.info("Memory forced merge completed in " + (elapsed / 1000.) + "s");
    }

    @Override
    public void close() throws IOException {
        try {
            this.storage.close();
        } finally {
            try {
                this.index.close();
            } finally {
                if (this.analysis != null)
                    this.analysis.shutdown();
            }
        }
    }

    protected void runAnalysis(ExecutorService executor, long maxToleratedMisalignment, int batchSize) throws IOException {
        Set<Bucket> buckets = storage.getUpdatedBuckets(maxToleratedMisalignment, batchSize);
        List<AnalysisTask> tasks = new ArrayList<>(buckets.size());

        for (Bucket bucket : buckets)
            tasks.add(new AnalysisTask(bucket));

        if (executor == null) {
            for (AnalysisTask task : tasks)
                task.run();
        } else {
            List<Future<Void>> results = new ArrayList<>(tasks.size());
            for (AnalysisTask task : tasks)
                results.add(executor.submit(task, null));

            for (Future<Void> future : results) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    // Ignore it
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException)
                        throw (RuntimeException) cause;
                    else
                        throw new Error("Unexpected exception", cause);
                }
            }
        }

        // Commit

        index.flush();

        for (AnalysisTask task : tasks) {
            try {
                storage.markUpdate(task.getBucket(), task.getSize());
            } catch (IOException e) {
                logger.error("Failed to mark update for bucket " + task.getBucket());
            }
        }
    }

    @Override
    public Collection<LogDataListener> getDataListeners() {
        return Collections.singleton(storage);
    }

    private class AnalysisThread extends Thread {

        private final int timeout;
        private final int batchSize;
        private final long maxToleratedMisalignment;

        private final ExecutorService executor;
        private final SynchronousQueue<Object> handoff;
        private boolean active = true;

        public AnalysisThread(AnalyzerConfig config) {
            this.timeout = config.getTimeout();
            this.batchSize = config.getBatchSize();
            this.maxToleratedMisalignment = config.getMaxToleratedMisalignment();

            int threads = config.getThreads();
            this.executor = Executors.newFixedThreadPool(config.getThreads());
            this.handoff = new SynchronousQueue<>();

            if (logger.isDebugEnabled()) {
                logger.debug("AnalysisThread started with: timeout=" + timeout + ", batchSize=" + batchSize +
                        ", maxToleratedMisalignment=" + maxToleratedMisalignment + ", threads=" + threads);
            }
        }

        public void run() {
            while (active) {
                try {
                    handoff.poll(timeout, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    // Ignore it
                }

                if (!active)
                    break;

                try {
                    runAnalysis(executor, maxToleratedMisalignment, batchSize);
                } catch (Exception e) {
                    logger.error("Failed to retrieve run analysis", e);
                }
            }
        }

        public void shutdown() {
            this.active = false;
            handoff.offer(new Object());

            try {
                executor.shutdownNow();
                executor.awaitTermination(timeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // ignore it
            }
        }
    }

    private class AnalysisTask implements Runnable {

        private final Bucket bucket;
        private long size = -1;

        public AnalysisTask(Bucket bucket) {
            this.bucket = bucket;
        }

        public long getSize() {
            return size < 0 ? bucket.getSize() : size;
        }

        public Bucket getBucket() {
            return bucket;
        }

        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();

                this.size = bucket.getSize();

                if (this.size == 0) {
                    // Deleted
                    index.delete(bucket.getId());
                } else {
                    Reader reader = new InputStreamReader(bucket.getContentStream(), UTF8Charset.get());
                    Document document = DocumentBuilder.newInstance(bucket.getOwner(), bucket.getId(), bucket.getLanguage(), reader);
                    index.update(document);
                }

                long elapsed = (long) ((System.currentTimeMillis() - start) / 100.);
                if (logger.isDebugEnabled())
                    logger.debug("Index of bucket " + bucket + " completed in " + (elapsed / 10.) + "s");
            } catch (Exception e) {
                logger.error("Failed to index bucket: " + bucket, e);
            }
        }
    }
}
