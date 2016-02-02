package eu.modernmt.processing.tokenizer.jflex;

import java.io.CharArrayReader;
import java.io.Reader;

/**
 * Created by davide on 01/02/16.
 */
public class AnnotatedString {

    private static final int WHITESPACE = 1;
    private static final int CONTROL = 2;

    private char[] chars;

    public AnnotatedString(String string) {
        this(string.toCharArray());
    }

    public AnnotatedString(char[] chars) {
        char[] tempChars = new char[chars.length];

        boolean start = true;
        boolean whitespace = false;

        int index = 0;

        for (char c : chars) {
            int type = 0;

            if ((0x0009 <= c && c <= 0x000D) || c == 0x0020 || c == 0x00A0 || c == 0x1680 ||
                    (0x2000 <= c && c <= 0x200A) || c == 0x202F || c == 0x205F || c == 0x3000) {
                type = WHITESPACE;
            } else if (c <= 0x001F) {
                type = CONTROL;
            }

            switch (type) {
                case WHITESPACE:
                    whitespace = true;
                    break;
                case CONTROL:
                    // Ignore it
                    break;
                default:
                    if (start) {
                        start = false;
                        whitespace = false;
                    } else if (whitespace) {
                        tempChars[index++] = ' ';
                        whitespace = false;
                    }

                    tempChars[index++] = c;
                    break;
            }
        }

        this.chars = new char[index + 2];
        this.chars[0] = ' ';
        System.arraycopy(tempChars, 0, this.chars, 1, index);
        this.chars[index + 1] = ' ';
    }

    public int length() {
        return chars.length;
    }

    public char[] getCharArray() {
        return chars;
    }

    public Reader getReader() {
        return new CharArrayReader(chars);
    }

    @Override
    public String toString() {
        return new String(chars);
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("'" + new AnnotatedString("Ciao\t\ncome stai?      \t \t") + "'");
    }
}
