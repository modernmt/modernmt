package eu.modernmt.contextanalyzer;

import eu.modernmt.contextanalyzer.lucene.ContextAnalyzerIndex;
import eu.modernmt.contextanalyzer.lucene.LuceneTranslationContext;
import eu.modernmt.contextanalyzer.lucene.ScoreDocument;
import eu.modernmt.model.context.TranslationContext;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.FileCorpus;
import eu.modernmt.model.corpus.StringCorpus;
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

    private final Logger logger = LogManager.getLogger(ContextAnalyzer.class);
    private ContextAnalyzerIndex index;

    public ContextAnalyzer(File indexPath) throws IOException {
        this.index = new ContextAnalyzerIndex(indexPath);
    }

    public void rebuild(File basePath, String lang) throws IOException {
        if (!basePath.exists())
            throw new FileNotFoundException(basePath.getAbsolutePath());

        logger.info("Rebuild ContextAnalyzer index...");

        String[] filter = lang == null ? null : new String[]{lang};
        Collection<File> files = FileUtils.listFiles(basePath, filter, false);

        ArrayList<Corpus> corpora = new ArrayList<>();
        for (File file : files)
            corpora.add(new FileCorpus(file));

        long now = System.currentTimeMillis();
        this.index.clear();
        this.index.addCorpora(corpora);
        long elapsed = (System.currentTimeMillis() - now) / 1000L;

        logger.info("ContextAnalyzer index rebuild completed in " + elapsed + "s.");
    }

    public TranslationContext getContext(String query, String lang, int limit) throws IOException {
        return getContext(new StringCorpus("QueryDoc", lang, query), limit);
    }

    public TranslationContext getContext(File source, String lang, int limit) throws IOException {
        return getContext(new FileCorpus(source, "QueryDoc", lang), limit);
    }

    public TranslationContext getContext(Corpus query, int limit) throws IOException {
        List<ScoreDocument> documents = this.index.getSimilarDocuments(query, limit);
        return new LuceneTranslationContext(documents);

    }
}
