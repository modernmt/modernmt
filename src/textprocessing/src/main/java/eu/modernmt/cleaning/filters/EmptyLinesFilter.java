package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.Filter;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by davide on 14/03/16.
 */
public class EmptyLinesFilter implements Filter {

    private static final Pattern WHITESPACE_REMOVER = Pattern.compile("\\s+");

    @Override
    public Initializer getInitializer() {
        return null;
    }

    private static boolean isBlankLine(String line) {
        return WHITESPACE_REMOVER.matcher(line).replaceAll("").isEmpty();
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) throws IOException {
        return !(isBlankLine(pair.source) || isBlankLine(pair.target));
    }

    @Override
    public void clear() {
        // Nothing to do
    }

}
