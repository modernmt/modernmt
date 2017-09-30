package eu.modernmt.decoder.neural.memory.lucene;

/**
 * Created by davide on 30/09/17.
 */
class HashGenerator {

    private static final String CHARS = "0123456789ABCDEF";

    public static String hash(String sentence, String translation) {
        if (sentence.compareTo(translation) > 0) {
            String tmp = sentence;
            sentence = translation;
            translation = tmp;
        }

        long h1 = get30BitHash(sentence);
        long h2 = get30BitHash(translation);

        long hash = (h1 << 30) + h2;

        char[] string = new char[17];

        toHex((int)(hash & 0xFFFFF), string, 12);
        hash >>>= 20;
        toHex((int)(hash & 0xFFFFF), string, 6);
        hash >>>= 20;
        toHex((int)(hash & 0xFFFFF), string, 0);

        string[5] = ' ';
        string[11] = ' ';

        return new String(string);
    }

    private static int get30BitHash(String sentence) {
        int hash = sentence.hashCode();

        if ((hash & 0x1) > 0)
            hash >>>= 1;
        hash >>>= 1;

        return hash & 0x3FFFFFFF;
    }

    private static void toHex(int b20, char[] dest, int offset) {
        for (int i = 5; i > 0; i--) {
            dest[offset + i - 1]= CHARS.charAt(b20 & 0xF);
            b20 >>>= 4;
        }
    }

}
