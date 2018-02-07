package eu.modernmt.processing.numbers.internal;

public class NumericUtils {

    public static char[] joinDigits(NumericPlaceholder... placeholders) {
        int length = 0;
        for (NumericPlaceholder e : placeholders)
            length += e.getDigits().length;

        char[] digits = new char[length];
        int i = 0;
        for (NumericPlaceholder e : placeholders) {
            char[] piece = e.getDigits();
            System.arraycopy(piece, 0, digits, i, piece.length);
            i += piece.length;
        }

        return digits;
    }

    public static String applyDigits(char[] digits, String text) {
        char[] chars = text.toCharArray();
        int index = 0;

        for (int i = 0; i < chars.length && index < digits.length; i++) {
            if (chars[i] >= '0' && chars[i] <= '9')
                chars[i] = digits[index++];
        }

        return new String(chars);
    }

    public static String obfuscate(String text) {
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= '0' && c <= '9')
                chars[i] = '?';
        }

        return new String(chars);
    }
}
