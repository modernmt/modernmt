package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.MultilingualCorpusFilter;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by davide on 28/08/17.
 */
public class PunctuationFilter implements MultilingualCorpusFilter {

    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("(_+)|(\\s+)|(\\.+)|(\\*+)|(…+)|(-+)");
    private static final Pattern LETTER_REGEX = Pattern.compile("[\\p{Digit}\\p{L}]");

    @Override
    public FilterInitializer getInitializer() {
        return null;
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) throws IOException {
        String source = normalize(pair.source);
        String target = normalize(pair.target);

        double sourceRatio = countLetters(source) / ((double) source.length());
        double targetRatio = countLetters(target) / ((double) target.length());

        return sourceRatio >= .5 && targetRatio >= .5;
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
