package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.CorpusFilter;
import eu.modernmt.lang.Language;

import java.util.regex.Pattern;

/**
 * Created by davide on 10/11/17.
 */
public class NumericTextFilter implements CorpusFilter {

    private static final Pattern REGEX = Pattern.compile("[0-9\\s]+");

    @Override
    public Initializer getInitializer() {
        return null;
    }

    @Override
    public boolean accept(Language language, String line, int index) {
        return !REGEX.matcher(line).matches();
    }

    @Override
    public void clear() {
        // Nothing to do
    }
}
