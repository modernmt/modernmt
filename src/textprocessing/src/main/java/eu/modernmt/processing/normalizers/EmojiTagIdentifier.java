package eu.modernmt.processing.normalizers;

import eu.modernmt.model.EmojiTag;
import eu.modernmt.model.Tag;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import eu.modernmt.processing.string.TokenFactory;

import java.util.Map;
import java.util.regex.Matcher;

/**
 * Created by nicola on 04/12/19.
 * <p>
 * An EmojiTagIdentifier is the object that, in the processing pipeline,
 * handles the identification of Emoji Tags and requests
 * to the StringBuider editor their replacement with a single white space.
 */
public class EmojiTagIdentifier extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    /**
     * A EMOJI_TAG_FACTORY is an implementation of Token Factory that creates EmojiTags
     */
    public static final TokenFactory TAG_FACTORY = new TokenFactory() {
        @Override
        public Tag build(String text, String placeholder, boolean hasLeftSpace, String rightSpace, int position) {
            return EmojiTag.fromText(text, hasLeftSpace, rightSpace, position);
        }

        @Override
        public String toString() {
            return "Emoji Tag Factory";
        }
    };

    /**
     * This method uses a Matcher to find all Emoji tags
     * in the current String of the input SentenceBuilder.
     * For each tag found, it requests the SentenceBuilder editor
     * to set a Tag Token and to replace it in the currentString with " "
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

        /*find all substrings matching Emoji tags in the SentenceBuilder current String*/
        Matcher m = EmojiTag.TagRegex.matcher(builder.toString());

        SentenceBuilder.Editor editor = builder.edit();

        /*for each tag found,
         * ask the creation of a separate Tag token
         * and replace the tag text on the StringBuilder currentString with " " */
        while (m.find()) {
            int start = m.start();
            int end = m.end();

            editor.setTag(start, end - start, " ", TAG_FACTORY);

        }

        editor.commit();
        return builder;
    }
}
