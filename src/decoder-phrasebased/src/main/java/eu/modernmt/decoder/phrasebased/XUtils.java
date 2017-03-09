package eu.modernmt.decoder.phrasebased;

import eu.modernmt.model.Alignment;
import eu.modernmt.model.Word;

/**
 * Created by davide on 20/09/16.
 */
class XUtils {

    public static int[] encode(Word[] words) {
        int[] ids = new int[words.length];
        for (int i = 0; i < ids.length; i++)
            ids[i] = words[i].getId();
        return ids;
    }

    public static int[] encode(Alignment alignment) {
        int size = alignment.size();

        int[] encoded = new int[size * 2];
        System.arraycopy(alignment.getSourceIndexes(), 0, encoded, 0, size);
        System.arraycopy(alignment.getTargetIndexes(), 0, encoded, size, size);

        return encoded;
    }

    public static Alignment decode(int[] encoded) {
        if (encoded == null || encoded.length == 0)
            return null;

        int size = encoded.length / 2;

        int[] source = new int[size];
        int[] target = new int[size];

        System.arraycopy(encoded, 0, source, 0, size);
        System.arraycopy(encoded, size, target, 0, size);

        return new Alignment(source, target);
    }

    public static String join(Word[] words) {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0)
                text.append(' ');

            text.append(Integer.toUnsignedString(words[i].getId()));
        }

        return text.toString();
    }

    public static Word[] explode(String text) {
        if (text.isEmpty())
            return new Word[0];

        String[] pieces = text.split(" +");
        Word[] words = new Word[pieces.length];

        for (int i = 0; i < pieces.length; i++) {
            String rightSpace = i < pieces.length - 1 ? " " : null;

            long id = Long.parseLong(pieces[i]);
            words[i] = new Word((int) (id), rightSpace);
        }

        return words;
    }

}
