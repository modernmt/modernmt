package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.MultilingualCorpusFilter;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.TranslationUnit;

/**
 * Created by davide on 10/11/17.
 */
public class VerbatimTranslationFilter implements MultilingualCorpusFilter {

    @Override
    public Initializer getInitializer() {
        return null;
    }

    @Override
    public boolean accept(TranslationUnit tu, int index) {
        String source = normalize(tu.source);
        String target = normalize(tu.target);

        return (source.length() < 20) || !(source.equals(target));
    }

    private static String normalize(String string) {
        return string.toLowerCase().replaceAll("\\s+", " ");
    }

    @Override
    public void clear() {
        // Nothing to do
    }
}
