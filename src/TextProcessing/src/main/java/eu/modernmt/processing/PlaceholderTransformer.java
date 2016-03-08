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
            int[] alignments = new int[source.length];
            Arrays.fill(alignments, -1);
            for (int[] a : translation.getAlignment())
                alignments[a[0]] = a[1];

            for (int i = 0; i < source.length; i++) {
                Token sourceToken = source[i];

                if (!(sourceToken instanceof PlaceholderToken))
                    continue;

                Token targetToken = alignments[i] < 0 ? null : target[alignments[i]];

                if (targetToken == null || !(targetToken instanceof PlaceholderToken))
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
