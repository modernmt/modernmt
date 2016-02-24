package eu.modernmt.context;

import eu.modernmt.context.lucene.ContextAnalyzerIndex;
import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.model.Corpus;
import eu.modernmt.model.impl.FileCorpus;
import eu.modernmt.model.impl.StringCorpus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    public void rebuild(Collection<? extends BilingualCorpus> documents, boolean useSourceLanguage) throws ContextAnalyzerException {
        ArrayList<Corpus> corpora = new ArrayList<>(documents.size());
        for (BilingualCorpus corpus : documents)
            corpora.add(useSourceLanguage ? corpus.getSourceCorpus() : corpus.getTargetCorpus());

        rebuild(corpora);
    }

    public void rebuild(Collection<? extends Corpus> documents) throws ContextAnalyzerException {
        logger.info("Rebuild ContextAnalyzer index...");

        long now = System.currentTimeMillis();
        this.index.clear();
        this.index.add(documents);
        long elapsed = (System.currentTimeMillis() - now) / 1000L;

        logger.info("ContextAnalyzer index rebuild completed in " + elapsed + "s.");
    }

    public List<ContextDocument> getContext(String query, Locale lang, int limit) throws ContextAnalyzerException {
        return getContext(new StringCorpus(null, lang, query), limit);
    }

    public List<ContextDocument> getContext(File source, Locale lang, int limit) throws ContextAnalyzerException {
        return getContext(new FileCorpus(source, null, lang), limit);
    }

    public List<ContextDocument> getContext(Corpus query, int limit) throws ContextAnalyzerException {
        return this.index.getSimilarDocuments(query, limit);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.index.close();
    }

}
