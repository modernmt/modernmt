package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.Filter;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by davide on 10/11/17.
 */
public class NumericTextFilter implements Filter {

    private static final Pattern REGEX = Pattern.compile("[0-9\\s]+");

    @Override
    public Initializer getInitializer() {
        return null;
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) throws IOException {
        return !REGEX.matcher(pair.source).matches() && !REGEX.matcher(pair.target).matches();
    }

    @Override
    public void clear() {
        // Nothing to do
    }
}
