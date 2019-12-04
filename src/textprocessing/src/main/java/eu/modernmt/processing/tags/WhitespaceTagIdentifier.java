package eu.modernmt.processing.tags;

import eu.modernmt.model.Tag;
import eu.modernmt.model.WhitespaceTag;
import eu.modernmt.processing.string.TokenFactory;

/**
 * Created by nicola on 04/12/19.
 * <p>
 * An WhitespaceTagIdentifier is the object that, in the processing pipeline,
 * handles the identification of Whitespace Tags and requests
 * to the StringBuider editor their replacement with a single white space.
 */
public class WhitespaceTagIdentifier extends TagIdentifier {

    private static final TokenFactory TAG_FACTORY = new TokenFactory() {
        @Override
        public Tag build(String text, String placeholder, boolean hasLeftSpace, String rightSpace, int position) {
            return WhitespaceTag.fromText(text, hasLeftSpace, rightSpace, position);
        }

        @Override
        public String toString() {
            return "WhitespaceTag Tag Factory";
        }
    };

    public WhitespaceTagIdentifier() {
        super(WhitespaceTag.TagRegex, TAG_FACTORY);
    }

}
