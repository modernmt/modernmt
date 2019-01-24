package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.CorpusNormalizer;

/**
 * Created by davide on 14/03/16.
 */
public class TooLongLinesFilter implements CorpusNormalizer {

    @Override
    public String normalize(String line) {
        return line.length() > 4096 ? "" : line;
    }

}
