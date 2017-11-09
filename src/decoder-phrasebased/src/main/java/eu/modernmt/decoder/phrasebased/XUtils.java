package eu.modernmt.decoder.phrasebased;

import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;

/**
 * Created by davide on 20/09/16.
 */
class XUtils {

    public static int[] encodeAlignment(Alignment alignment) {
        int size = alignment.size();

        int[] encoded = new int[size * 2];
        System.arraycopy(alignment.getSourceIndexes(), 0, encoded, 0, size);
        System.arraycopy(alignment.getTargetIndexes(), 0, encoded, size, size);

        return encoded;
    }

    public static Alignment decodeAlignment(int[] encoded) {
        if (encoded == null || encoded.length == 0)
            return null;

        int size = encoded.length / 2;

        int[] source = new int[size];
        int[] target = new int[size];

        System.arraycopy(encoded, 0, source, 0, size);
        System.arraycopy(encoded, size, target, 0, size);

        return new Alignment(source, target);
    }

    public static String encodeSentence(Sentence sentence) {
        return TokensOutputStream.serialize(sentence, false, true);
    }

    public static Word[] explode(String text) {
        if (text.isEmpty())
            return new Word[0];

        String[] parts = TokensOutputStream.deserialize(text);
        Word[] words = new Word[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String rightSpace = i < parts.length - 1 ? " " : null;
            words[i] = new Word(parts[i], rightSpace);
        }

        return words;
    }

}
