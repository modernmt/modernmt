package eu.modernmt.processing.numbers;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by andrearossi on 06/03/17.
 * <p>
 * A NumericPlaceholderNormalizer has the responsibility
 * to find word numbers in the StentenceBuilder current string
 * and to ask their replacement with zeroes.
 * <p>
 * Replacing numbers with zeroes is necessary because
 * their variability would lead to extremely low probabilities
 * in our Language Models.
 */
public class NumericPlaceholderNormalizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    /*the numeric pattern we use is any sequence of chars among 0 and 9*/
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("[0-9]+");

    /**
     * This method uses a Matcher to find all number words
     * in the current String of the input SentenceBuilder.
     * For each number found, it requests the SentenceBuilder editor
     * to replace it with an equally long string of zeroes.
     *
     * @param builder  a SentenceBuilder that holds the input String
     *                 and can pass to clients an Editor to process it
     * @param metadata additional information on the current pipe
     *                 (not used in this specific operation)
     * @return the SentenceBuilder received as a parameter;
     * its internal state has been updated by the execution of the call() method
     * @throws ProcessingException
     */
    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) throws ProcessingException {
        SentenceBuilder.Editor editor = builder.edit();
        Matcher matcher = NUMERIC_PATTERN.matcher(builder.toString());

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            String zeroesString = StringUtils.repeat("0", end - start);
            editor.replace(start, end - start, zeroesString);
        }
        editor.commit();
        return builder;
    }
}
