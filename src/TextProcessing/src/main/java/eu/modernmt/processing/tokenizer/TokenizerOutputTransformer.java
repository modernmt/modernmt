package eu.modernmt.processing.tokenizer;

import java.util.BitSet;
import java.util.List;

/**
 * Created by davide on 19/02/16.
 */
@Deprecated
public class TokenizerOutputTransformer {

    @Deprecated
    public static void transform(TokenizedString text, String[] tokens) {
        int length = text.string.length();

        int stringIndex = 0;
        int lastPosition = 0;

        for (String token : tokens) {
            int tokenPos = text.string.indexOf(token, stringIndex);
            stringIndex = tokenPos + token.length();

            if (tokenPos != lastPosition)
                text.setToken(lastPosition, tokenPos);

            lastPosition = tokenPos + token.length();
            if (lastPosition < length)
                text.setToken(tokenPos, lastPosition);
        }
    }

    @Deprecated
    public static void transform(TokenizedString string, List<String> tokens) {
        transform(string, tokens.toArray(new String[tokens.size()]));
    }

    private static void printDebug(String string, BitSet bitSet) {
        for (int i = 0; i < string.length(); i++) {
            if (bitSet.get(i))
                System.out.print('|');
            System.out.print(string.charAt(i));
        }

        System.out.println();
    }

}
