package eu.modernmt.context;

import eu.modernmt.context.lucene.ContextAnalyzerIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 02/12/15.
 */
public class ContextAnalyzer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    protected ContextAnalyzerIndex index;

    public ContextAnalyzer(File indexPath) throws IOException {
        this.index = new ContextAnalyzerIndex(indexPath);
    }

    public void rebuild(Collection<? extends IndexSourceDocument> documents) throws ContextAnalyzerException {
        logger.info("Rebuild ContextAnalyzer index...");

        long now = System.currentTimeMillis();
        this.index.clear();
        this.index.add(documents);
        long elapsed = (System.currentTimeMillis() - now) / 1000L;

        logger.info("ContextAnalyzer index rebuild completed in " + elapsed + "s.");
    }

    public List<ContextDocument> getContext(String query, Locale lang, int limit) throws ContextAnalyzerException {
        return getContext(IndexSourceDocument.fromString(query, lang), limit);
    }

    public List<ContextDocument> getContext(File source, Locale lang, int limit) throws ContextAnalyzerException {
        return getContext(IndexSourceDocument.fromFile(source, lang), limit);
    }

    public List<ContextDocument> getContext(IndexSourceDocument query, int limit) throws ContextAnalyzerException {
        return this.index.getSimilarDocuments(query, limit);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.index.close();
    }

}
