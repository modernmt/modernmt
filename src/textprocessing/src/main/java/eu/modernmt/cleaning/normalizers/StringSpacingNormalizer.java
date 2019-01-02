package eu.modernmt.cleaning.normalizers;

import eu.modernmt.cleaning.CorpusNormalizer;
import eu.modernmt.processing.normalizers.ControlCharsRemover;

import java.util.regex.Pattern;

/**
 * Created by davide on 17/11/16.
 */
public class StringSpacingNormalizer implements CorpusNormalizer {

    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    @Override
    public String normalize(String line) {
        line = WHITESPACES.matcher(line).replaceAll(" ");
        line = line.trim();
        return line;
    }
}
