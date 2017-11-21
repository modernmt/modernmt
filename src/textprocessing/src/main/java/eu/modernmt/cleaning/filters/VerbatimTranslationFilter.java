package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.MultilingualCorpusFilter;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;

/**
 * Created by davide on 10/11/17.
 */
public class VerbatimTranslationFilter implements MultilingualCorpusFilter {

    @Override
    public FilterInitializer getInitializer() {
        return null;
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) throws IOException {
        String source = normalize(pair.source);
        String target = normalize(pair.target);

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
