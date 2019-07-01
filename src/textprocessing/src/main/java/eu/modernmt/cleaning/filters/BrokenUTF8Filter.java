package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.CorpusFilter;
import eu.modernmt.lang.Language;

/**
 * Created by davide on 14/03/16.
 */
public class BrokenUTF8Filter implements CorpusFilter {

    @Override
    public Initializer getInitializer(Language language) {
        return null;
    }

    @Override
    public boolean accept(String line, int index) {
        return line.indexOf('\uFFFD') == -1 && line.indexOf('\uFFFE') == -1 && line.indexOf('\uFFFF') == -1;
    }

    @Override
    public void clear() {
        // Nothing to do
    }

}
