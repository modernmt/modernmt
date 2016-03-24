package eu.modernmt.processing;

import eu.modernmt.model.PlaceholderToken;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by davide on 02/03/16.
 */
public class PlaceholderTransformer implements TextProcessor<Translation, Translation> {

    @Override
    public Translation call(Translation translation) throws ProcessingException {
        Token[] source = translation.getSource().getWords();
        Token[] target = translation.getWords();

        if (translation.hasAlignment()) {
            for (int[] pair : translation.getAlignment()) {
                Token sourceToken = source[pair[0]];
                Token targetToken = target[pair[1]];

                if (!(sourceToken instanceof PlaceholderToken) || !(targetToken instanceof PlaceholderToken))
                    continue;

                ((PlaceholderToken) targetToken).applyTransformation((PlaceholderToken) sourceToken);
            }
        }

        for (Token token : target) {
            if (token instanceof PlaceholderToken) {
                PlaceholderToken ptoken = ((PlaceholderToken) token);

                if (!ptoken.hasText())
                    ptoken.applyFallbackTransformation();
            }
        }

        return translation;
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }

}
