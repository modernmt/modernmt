package eu.modernmt.processing.xmessage;

import eu.modernmt.model.Word;
import eu.modernmt.processing.WordTransformationFactory;

/**
 * Created by davide on 08/04/16.
 */
public class XMessageWordTransformer implements WordTransformationFactory.WordTransformer {

    @Override
    public boolean match(Word word) {
        return XFormat.PLACEHOLDER_PATTERN.matcher(word.getPlaceholder()).matches();
    }

    @Override
    public Word setupTransformation(Word word) {
        return new XFormatWord(word.getText(), word.getPlaceholder(), word.getRightSpace(), word.isRightSpaceRequired());
    }

}
