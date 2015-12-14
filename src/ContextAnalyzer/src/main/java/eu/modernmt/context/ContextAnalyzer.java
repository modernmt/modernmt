package eu.modernmt.context;

import eu.modernmt.context.lucene.ContextAnalyzerIndex;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 02/12/15.
 */
public class ContextAnalyzer {

    protected final Logger logger = LogManager.getLogger(getClass());
    protected ContextAnalyzerIndex index;

    public ContextAnalyzer(File indexPath) throws IOException {
        this.index = new ContextAnalyzerIndex(indexPath);
    }

    public void rebuild(Collection<? extends IndexSourceDocument> documents) throws IOException {
        logger.info("Rebuild ContextAnalyzer index...");

        long now = System.currentTimeMillis();
        this.index.clear();
        this.index.add(documents);
        long elapsed = (System.currentTimeMillis() - now) / 1000L;

        logger.info("ContextAnalyzer index rebuild completed in " + elapsed + "s.");
    }

    public List<ContextDocument> getContext(String query, Locale lang, int limit) throws IOException {
        return getContext(IndexSourceDocument.fromString(query, lang), limit);
    }

    public List<ContextDocument> getContext(File source, Locale lang, int limit) throws IOException {
        FileReader reader = null;
        try {
            reader = new FileReader(source);
            return getContext(IndexSourceDocument.fromReader(reader, lang), limit);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public List<ContextDocument> getContext(Reader reader, Locale lang, int limit) throws IOException {
        return getContext(IndexSourceDocument.fromReader(reader, lang), limit);
    }

    public List<ContextDocument> getContext(IndexSourceDocument query, int limit) throws IOException {
        return this.index.getSimilarDocuments(query, limit);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.index.close();
    }
}
