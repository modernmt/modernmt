package eu.modernmt.context;

import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.model.Corpus;
import eu.modernmt.model.impl.FileCorpus;
import eu.modernmt.model.impl.StringCorpus;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 02/12/15.
 */
public abstract class ContextAnalyzer implements Closeable, AutoCloseable {

    public void rebuild(Collection<? extends BilingualCorpus> documents, boolean useSourceLanguage) throws ContextAnalyzerException {
        ArrayList<Corpus> corpora = new ArrayList<>(documents.size());
        for (BilingualCorpus corpus : documents)
            corpora.add(useSourceLanguage ? corpus.getSourceCorpus() : corpus.getTargetCorpus());

        rebuild(corpora);
    }

    public abstract void rebuild(Collection<? extends Corpus> documents) throws ContextAnalyzerException;

    public List<ContextDocument> getContext(String query, Locale lang, int limit) throws ContextAnalyzerException {
        return getContext(new StringCorpus(null, lang, query), limit);
    }

    public List<ContextDocument> getContext(File source, Locale lang, int limit) throws ContextAnalyzerException {
        return getContext(new FileCorpus(source, null, lang), limit);
    }

    public abstract List<ContextDocument> getContext(Corpus query, int limit) throws ContextAnalyzerException;

}
