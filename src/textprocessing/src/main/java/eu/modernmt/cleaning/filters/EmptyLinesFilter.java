package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.BilingualCorpusFilter;
import eu.modernmt.model.corpus.BilingualCorpus;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by davide on 14/03/16.
 */
public class EmptyLinesFilter implements BilingualCorpusFilter {

    private static final Pattern WHITESPACE_REMOVER = Pattern.compile("\\s+");

    @Override
    public FilterInitializer getInitializer() {
        return null;
    }

    private static boolean isBlankLine(String line) {
        return WHITESPACE_REMOVER.matcher(line).replaceAll("").isEmpty();
    }

    @Override
    public boolean accept(BilingualCorpus.StringPair pair) throws IOException {
        return !(isBlankLine(pair.source) || isBlankLine(pair.target));
    }

}
