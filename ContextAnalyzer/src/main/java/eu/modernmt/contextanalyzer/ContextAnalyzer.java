package eu.modernmt.contextanalyzer;

import eu.modernmt.contextanalyzer.lucene.LuceneController;
import eu.modernmt.contextanalyzer.lucene.ScoreDocument;
import eu.modernmt.corpus.Corpus;
import eu.modernmt.corpus.FileCorpus;
import eu.modernmt.corpus.SimpleCorpus;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by davide on 10/07/15.
 */
public final class ContextAnalyzer {

    private static final Logger logger = LogManager.getLogger(ContextAnalyzer.class);
    private static ContextAnalyzer instance = null;

    public static void setConfig(ContextAnalyzerConfig config) {
        if (instance != null)
            throw new IllegalStateException("Context analyzer config already set");

        instance = new ContextAnalyzer(config);
    }

    public static ContextAnalyzer getInstance() {
        if (instance == null)
            throw new IllegalStateException("Context analyzer config not set");

        return instance;
    }

    private final LuceneController lucene;

    private ContextAnalyzer(ContextAnalyzerConfig config) {
        this.lucene = new LuceneController(config.indexPath);
    }

    public void reindex(File basePath, String lang) throws IOException {
        logger.info("Refreshing Lucene index...");

        if (!basePath.exists())
            throw new FileNotFoundException(basePath.toString());

        String[] filter = lang == null ? null : new String[]{lang};
        Collection<File> files = FileUtils.listFiles(basePath, filter, false);

        ArrayList<Corpus> corpora = new ArrayList<>();
        for (File file : files)
            corpora.add(new FileCorpus(file));

        long now = System.currentTimeMillis();
        lucene.reindex(corpora);
        long elapsed = (System.currentTimeMillis() - now) / 1000L;

        logger.info("Lucene index refresh completed in " + elapsed + "s.");
    }

    public Context getContext(String query, String lang, int limit) throws IOException {
        return getContext(new SimpleCorpus("QueryDoc", lang, query), limit);
    }

    public Context getContext(File source, String lang, int limit) throws IOException {
        return getContext(new FileCorpus(source, "QueryDoc", lang), limit);
    }

    public Context getContext(Corpus query, int limit) throws IOException {
        long start = System.currentTimeMillis();
        List<ScoreDocument> documents = lucene.getSimilarDocuments(query, limit);
        double elapsedTime = (System.currentTimeMillis() - start);

        return new Context(documents, elapsedTime / 1000.);
    }
}
