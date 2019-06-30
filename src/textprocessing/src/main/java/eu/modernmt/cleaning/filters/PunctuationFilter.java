package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.CorpusFilter;
import eu.modernmt.lang.Language2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by davide on 28/08/17.
 */
public class PunctuationFilter implements CorpusFilter {

    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("(_+)|(\\s+)|(\\.+)|(\\*+)|(…+)|(-+)");
    private static final Pattern LETTER_REGEX = Pattern.compile("[\\p{Digit}\\p{L}]");

    @Override
    public Initializer getInitializer(Language2 language) {
        return null;
    }

    @Override
    public boolean accept(String line, int index) {
        String norm = normalize(line);

        double sourceRatio = countLetters(norm) / ((double) norm.length());
        return sourceRatio >= .5;
    }

    private static String normalize(String string) {
        return PLACEHOLDER_REGEX.matcher(string).replaceAll("_");
    }

    private static int countLetters(String string) {
        Matcher matcher = LETTER_REGEX.matcher(string);

        int count = 0;
        while (matcher.find())
            count++;

        return count;
    }

    @Override
    public void clear() {

    }

}
