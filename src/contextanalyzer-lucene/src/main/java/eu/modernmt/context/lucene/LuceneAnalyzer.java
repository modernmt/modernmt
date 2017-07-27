package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.lucene.storage.CorporaStorage;
import eu.modernmt.context.lucene.storage.Options;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.model.LanguagePair;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.impl.StringCorpus;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 09/05/16.
 */
public class LuceneAnalyzer implements ContextAnalyzer {

    private final Logger logger = LogManager.getLogger(LuceneAnalyzer.class);

    private final ContextAnalyzerIndex index;
    private final CorporaStorage storage;

    public LuceneAnalyzer(File indexPath) throws IOException {
        this(indexPath, new Options());
    }

    public LuceneAnalyzer(File indexPath, Options options) throws IOException {
        this.index = new ContextAnalyzerIndex(new File(indexPath, "index"));
        this.storage = new CorporaStorage(new File(indexPath, "storage"), options, this.index);
    }

    @Override
    public void add(LanguagePair direction, Domain domain, Corpus corpus) throws ContextAnalyzerException {
        HashMap<Domain, Corpus> map = new HashMap<>(1);
        map.put(domain, corpus);

        this.add(direction, map);
    }

    @Override
    public void add(LanguagePair direction, Map<Domain, Corpus> corpora) throws ContextAnalyzerException {
        for (Map.Entry<Domain, Corpus> entry : corpora.entrySet()) {
            long id = entry.getKey().getId();

            try {
                this.storage.bulkInsert(direction, id, entry.getValue());
            } catch (IOException e) {
                throw new ContextAnalyzerException("Unable to add domain " + id, e);
            }
        }

        try {
            this.storage.flushToDisk(false, true);
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to write memory index to disk", e);
        }
    }

    @Override
    public ContextVector getContextVector(LanguagePair direction, String query, int limit) throws ContextAnalyzerException {
        return getContextVector(direction, new StringCorpus(null, null, query), limit);
    }

    @Override
    public ContextVector getContextVector(LanguagePair direction, File source, int limit) throws ContextAnalyzerException {
        return getContextVector(direction, new FileCorpus(source, null, null), limit);
    }

    @Override
    public ContextVector getContextVector(LanguagePair direction, Corpus query, int limit) throws ContextAnalyzerException {
        return this.index.getSimilarDocuments(direction, query, limit);
    }

    @Override
    public void close() throws IOException {
        try {
            this.storage.shutdown();
            this.storage.awaitTermination(TimeUnit.SECONDS, 2);
        } catch (InterruptedException e) {
            // Ignore it
        } finally {
            this.index.close();
        }
    }

    // UpdateListener

    @Override
    public void onDataReceived(List<TranslationUnit> batch) throws ContextAnalyzerException {
        try {
            storage.onDataReceived(batch);
        } catch (InterruptedException | IOException e) {
            throw new ContextAnalyzerException(e);
        }
    }

    @Override
    public void onDelete(Deletion deletion) throws ContextAnalyzerException {
        boolean deletedFromIndex = false;
        boolean deletedFromStorage = false;

        try {
            storage.onDelete(deletion);
            deletedFromStorage = true;
        } catch (InterruptedException | IOException e) {
            logger.error("Storage delete failed for domain " + deletion.domain, e);
        }

        try {
            index.delete(deletion.domain);
            index.flush();

            deletedFromIndex = true;
        } catch (ContextAnalyzerException e) {
            logger.error("Index delete failed for domain " + deletion.domain, e);
        }

        if (!deletedFromIndex || !deletedFromStorage)
            throw new ContextAnalyzerException("Failed to delete domain " + deletion.domain);

        logger.info("Deleted domain " + deletion.domain);
    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return storage.getLatestChannelPositions();
    }

}
