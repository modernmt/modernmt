package eu.modernmt.data;

import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.TranslationUnit;

/**
 * Created by davide on 30/09/17.
 */
public class HashGenerator {

    private static final long TRUE_HASH_SIZE = 1L << 40;
    private static final long TRUE_HASH_MASK = TRUE_HASH_SIZE - 1;
    private static final long FNV_PRIME = 1099511628211L;
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final String CHARS = "0123456789ABCDEF";

    public static String hash(LanguageDirection language, String tuid) {
        String text = language.source.toLanguageTag() + ',' + language.target.toLanguageTag() + ',' + tuid;

        byte[] original = text.getBytes(UTF8Charset.get());
        int size = (original.length + 1) / 2;

        int ia = 0;
        int ib = 0;
        byte[] a = new byte[size];
        byte[] b = new byte[size];

        for (int i = 0; i < original.length; i++) {
            if (i % 2 == 0)
                a[ia++] = original[i];
            else
                b[ib++] = original[i];
        }

        long h1_40bit = FNV_1a_lazy_mod_mapping(a);
        long h2_40bit = FNV_1a_lazy_mod_mapping(b);

        return toString(h1_40bit, h2_40bit);
    }

    public static String hash(TranslationUnit tu) {
        return hash(tu.language, tu.source, tu.target);
    }

    public static String hash(LanguageDirection language, String sentence, String translation) {
        sentence = language.source.toLanguageTag() + "|||" + sentence;
        translation = language.target.toLanguageTag() + "|||" + translation;

        long h1_40bit = FNV_1a_lazy_mod_mapping(sentence);
        long h2_40bit = FNV_1a_lazy_mod_mapping(translation);

        return toString(h1_40bit, h2_40bit);
    }

    private static String toString(long h1_40bit, long h2_40bit) {
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
        return FNV_1a_lazy_mod_mapping(sentence.getBytes(UTF8Charset.get()));
    }

    private static long FNV_1a_lazy_mod_mapping(byte[] bytes) {
        long hash = FNV_OFFSET_BASIS;
        for (byte b : bytes) {
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
