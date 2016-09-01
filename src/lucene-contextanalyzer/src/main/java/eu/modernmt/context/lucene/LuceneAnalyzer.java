package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.model.corpus.Corpus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Created by davide on 09/05/16.
 */
public class LuceneAnalyzer extends ContextAnalyzer implements AutoCloseable {

    private final Logger logger = LogManager.getLogger(ContextAnalyzer.class);
    protected ContextAnalyzerIndex index;

    public LuceneAnalyzer(File indexPath) throws IOException {
        this.index = new ContextAnalyzerIndex(indexPath);
    }

    @Override
    public void rebuild(Collection<? extends Corpus> documents) throws ContextAnalyzerException {
        logger.info("Rebuild ContextAnalyzer index...");

        long now = System.currentTimeMillis();
        this.index.clear();
        this.index.add(documents);
        long elapsed = (System.currentTimeMillis() - now) / 1000L;

        logger.info("ContextAnalyzer index rebuild completed in " + elapsed + "s.");
    }

    @Override
    public List<ContextDocument> getContext(Corpus query, int limit) throws ContextAnalyzerException {
        return this.index.getSimilarDocuments(query, limit);
    }

    @Override
    public void close() throws IOException {
        this.index.close();
    }
}
