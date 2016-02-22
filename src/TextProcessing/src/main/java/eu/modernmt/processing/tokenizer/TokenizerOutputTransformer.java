package eu.modernmt.processing.tokenizer;

import java.util.BitSet;
import java.util.List;

/**
 * Created by davide on 19/02/16.
 */
@Deprecated
public class TokenizerOutputTransformer {

    @Deprecated
    public static BitSet transform(String string, String[] tokens) {
        int length = string.length();

        BitSet boundaries = new BitSet(length);
        int stringIndex = 0;
        int lastPosition = 0;

        for (String token : tokens) {
            int tokenPos = string.indexOf(token, stringIndex);
            stringIndex = tokenPos + token.length();

            if (tokenPos != lastPosition) {
                boundaries.set(lastPosition);
                boundaries.set(tokenPos);
            }

            lastPosition = tokenPos + token.length();
            if (lastPosition < length)
                boundaries.set(lastPosition);
        }

        return boundaries;
    }

    @Deprecated
    public static BitSet transform(String string, List<String> tokens) {
        return transform(string, tokens.toArray(new String[tokens.size()]));
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
