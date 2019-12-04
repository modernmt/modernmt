package eu.modernmt.processing.tags;

import eu.modernmt.model.Tag;
import eu.modernmt.model.XMLTag;
import eu.modernmt.processing.string.TokenFactory;


/**
 * Created by andrea on 02/03/17.
 * <p>
 * An XMLTagIdentifier is the object that, in the processing pipeline,
 * handles the identification of XML Tags and requests
 * to the StringBuider editor their replacement with a single white space.
 */
public class XMLTagIdentifier extends TagIdentifier {

    private static final TokenFactory TAG_FACTORY = new TokenFactory() {
        @Override
        public Tag build(String text, String placeholder, boolean hasLeftSpace, String rightSpace, int position) {
            return XMLTag.fromText(text, hasLeftSpace, rightSpace, position);
        }

        @Override
        public String toString() {
            return "XML Tag Factory";
        }
    };

    public XMLTagIdentifier() {
        super(XMLTag.TagRegex, TAG_FACTORY);
    }

}
