package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.lucene.analysis.ContextAnalyzerIndex;
import eu.modernmt.context.lucene.analysis.DocumentBuilder;
import eu.modernmt.context.lucene.storage.Bucket;
import eu.modernmt.context.lucene.storage.CorporaStorage;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.LanguagePair;
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
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 09/05/16.
 */
public class LuceneAnalyzer implements ContextAnalyzer, DataListenerProvider {

    private final Logger logger = LogManager.getLogger(LuceneAnalyzer.class);

    private final ContextAnalyzerIndex index;
    private final CorporaStorage storage;
    private final AnalysisThread analysis;

    public LuceneAnalyzer(File indexPath) throws IOException {
        this(indexPath, new AnalysisOptions());
    }

    public LuceneAnalyzer(File indexPath, AnalysisOptions options) throws IOException {
        this.index = new ContextAnalyzerIndex(new File(indexPath, "index"));
        this.storage = new CorporaStorage(new File(indexPath, "storage"));

        if (options.enabled) {
            this.analysis = new AnalysisThread(options);
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
    public ContextVector getContextVector(UUID user, LanguagePair direction, String query, int limit) throws ContextAnalyzerException {
        return getContextVector(user, direction, new StringCorpus(null, direction.source, query), limit);
    }

    @Override
    public ContextVector getContextVector(UUID user, LanguagePair direction, File source, int limit) throws ContextAnalyzerException {
        return getContextVector(user, direction, new FileCorpus(source, null, direction.source), limit);
    }

    @Override
    public ContextVector getContextVector(UUID user, LanguagePair direction, Corpus query, int limit) throws ContextAnalyzerException {
        try {
            return this.index.getContextVector(user, direction, query, limit);
        } catch (IOException e) {
            throw new ContextAnalyzerException("Failed to calculate context-vector due an internal error", e);
        }
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

    @Override
    public Collection<DataListener> getDataListeners() {
        return Collections.singleton(storage);
    }

    private class AnalysisThread extends Thread {

        private final int timeout;
        private final int batchSize;
        private final long maxToleratedMisalignment;

        private final ExecutorService executor;
        private final SynchronousQueue<Object> handoff;
        private boolean active = true;

        public AnalysisThread(AnalysisOptions options) {
            this.timeout = options.timeout;
            this.batchSize = options.batchSize;
            this.maxToleratedMisalignment = options.maxToleratedMisalignment;

            this.executor = Executors.newFixedThreadPool(options.threads);
            this.handoff = new SynchronousQueue<>();
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
                    for (Bucket bucket : storage.getUpdatedBuckets(maxToleratedMisalignment, batchSize))
                        executor.execute(new AnalysisTask(bucket));
                } catch (IOException e) {
                    logger.error("Failed to retrieve updated buckets", e);
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

        public AnalysisTask(Bucket bucket) {
            this.bucket = bucket;
        }

        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();

                long size = bucket.getSize();

                if (size == 0) {
                    // Deleted
                    index.delete(bucket.getId());
                } else {
                    Reader reader = new InputStreamReader(bucket.getContentStream(), UTF8Charset.get());
                    Document document = DocumentBuilder.newInstance(bucket.getOwner(), bucket.getId(), bucket.getLanguage(), reader);
                    index.update(document);
                }

                storage.markUpdate(bucket, size);

                long elapsed = System.currentTimeMillis() - start;
                logger.info("Index of bucket " + bucket + " completed in " + (elapsed / 1000) + "s");
            } catch (Exception e) {
                logger.error("Failed to index bucket: " + bucket, e);
            }
        }

    }
}
