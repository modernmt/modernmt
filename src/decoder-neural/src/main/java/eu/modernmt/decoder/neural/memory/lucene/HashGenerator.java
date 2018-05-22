package eu.modernmt.decoder.neural.memory.lucene;

import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.LanguagePair;

/**
 * Created by davide on 30/09/17.
 */
class HashGenerator {

    private static final long TRUE_HASH_SIZE = 1L << 40;
    private static final long TRUE_HASH_MASK = TRUE_HASH_SIZE - 1;
    private static final long FNV_PRIME = 1099511628211L;
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final String CHARS = "0123456789ABCDEF";

    public static String hash(LanguagePair direction, String sentence, String translation) {
        String l1 = direction.source.toLanguageTag();
        String l2 = direction.target.toLanguageTag();

        if (l1.compareTo(l2) > 0) {
            String tmp = sentence;
            sentence = translation;
            translation = tmp;
        }

        long h1_40bit = FNV_1a_lazy_mod_mapping(sentence);
        long h2_40bit = FNV_1a_lazy_mod_mapping(translation);

        char[] string = new char[23];

        toHex((int) ((h1_40bit >>> 20) & 0xFFFFF), string, 0);
        string[5] = ' ';
        toHex((int) (h1_40bit & 0xFFFFF), string, 6);
        string[11] = ' ';
        toHex((int) ((h2_40bit >>> 20) & 0xFFFFF), string, 12);
        string[17] = ' ';
        toHex((int) (h2_40bit & 0xFFFFF), string, 18);

        return new String(string);
    }

    private static long FNV_1a_lazy_mod_mapping(String sentence) {
        long hash = FNV_OFFSET_BASIS;
        for (byte b : sentence.getBytes(UTF8Charset.get())) {
            hash ^= (b & 0xff);
            hash *= FNV_PRIME;
        }

        return (hash % TRUE_HASH_SIZE) & TRUE_HASH_MASK;
    }

    private static void toHex(int b20, char[] dest, int offset) {
        for (int i = 5; i > 0; i--) {
            dest[offset + i - 1] = CHARS.charAt(b20 & 0xF);
            b20 >>>= 4;
        }
    }

}
