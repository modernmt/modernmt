package eu.modernmt.processing.tags;

import eu.modernmt.model.EmojiTag;
import eu.modernmt.model.Tag;
import eu.modernmt.processing.string.TokenFactory;

/**
 * Created by nicola on 04/12/19.
 * <p>
 * An EmojiTagIdentifier is the object that, in the processing pipeline,
 * handles the identification of Emoji Tags and requests
 * to the StringBuider editor their replacement with a single white space.
 */
public class EmojiTagIdentifier extends TagIdentifier {

    private static final TokenFactory TAG_FACTORY = new TokenFactory() {
        @Override
        public Tag build(String text, String placeholder, String leftSpace, String rightSpace, int position) {
            return EmojiTag.fromText(text, leftSpace, rightSpace, position);
        }

        @Override
        public String toString() {
            return "Emoji Tag Factory";
        }
    };

    public EmojiTagIdentifier() {
        super(EmojiTag.TagRegex, TAG_FACTORY);
    }

}
