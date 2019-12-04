package eu.modernmt.processing.tags;

import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import eu.modernmt.processing.string.TokenFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TagIdentifier extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    private final Pattern pattern;
    private final TokenFactory tokenFactory;
    private final String replacement;

    protected TagIdentifier(Pattern pattern, TokenFactory tokenFactory) {
        this(pattern, tokenFactory, " ");
    }

    protected TagIdentifier(Pattern pattern, TokenFactory tokenFactory, String replacement) {
        this.pattern = pattern;
        this.tokenFactory = tokenFactory;
        this.replacement = replacement;
    }

    /**
     * This method uses a Matcher to find all  tags
     * in the current String of the input SentenceBuilder.
     * For each tag found, it requests the SentenceBuilder editor
     * to set a Tag Token and to replace it in the currentString with the replacement
     *
     * @param builder  a SentenceBuilder that holds the input String
     *                 and can pass to clients an Editor to process it
     * @param metadata additional information on the current pipe
     *                 (not used in this specific operation)
     * @return the SentenceBuilder received as a parameter;
     * its internal state has been updated by the queue of the call() method
     */
    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) {
        Matcher m = pattern.matcher(builder.toString());

        SentenceBuilder.Editor editor = builder.edit();

        /*for each tag found,
         * ask the creation of a separate Tag token
         * and replace the tag text on the StringBuilder currentString with replacement */
        while (m.find()) {
            int start = m.start();
            int end = m.end();

            editor.setTag(start, end - start, replacement, tokenFactory);

        }

        editor.commit();
        return builder;
    }
}
