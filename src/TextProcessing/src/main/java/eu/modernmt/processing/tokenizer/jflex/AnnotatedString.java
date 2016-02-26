package eu.modernmt.processing.tokenizer.jflex;

import java.io.CharArrayReader;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Created by davide on 01/02/16.
 */
public class AnnotatedString {

    public static final byte SPLIT_FLAG = (byte) (1);
    public static final byte PROTECTED_FLAG = (byte) (1 << 1);

    private static final int WHITESPACE = 1;
    private static final int CONTROL = 2;
    private static final int BREAK = 3;

    private char[] chars;
    private byte[] flags;
    private int length;

    public AnnotatedString(String string) {
        this(string.toCharArray());
    }

    public AnnotatedString(char[] source) {
        this.chars = new char[source.length + 2];
        this.flags = new byte[source.length + 3];

        boolean start = true;
        boolean whitespace = false;

        int index = 1;

        for (char c : source) {
            int type = 0;

            if ((0x0009 <= c && c <= 0x000D) || c == 0x0020 || c == 0x00A0 || c == 0x1680 ||
                    (0x2000 <= c && c <= 0x200A) || c == 0x202F || c == 0x205F || c == 0x3000) {
                type = WHITESPACE;
            } else if (c <= 0x001F) {
                type = CONTROL;
            } else if (c != '-' && !Character.isLetterOrDigit(c)) {
                type = BREAK;
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
                        this.chars[index] = ' ';
                        this.flags[index] = this.flags[index + 1] = SPLIT_FLAG;

                        whitespace = false;
                        index++;
                    }

                    this.chars[index] = c;
                    if (type == BREAK) {
                        this.flags[index] = this.flags[index + 1] = SPLIT_FLAG;
                    }

                    index++;
                    break;
            }
        }

        this.length = index + 1;
        this.chars[0] = this.chars[this.length - 1] = ' ';
        this.flags[this.length] = SPLIT_FLAG;
    }

    public void protect(int start, int end) {
        for (int i = start; i < end; i++) {
            if (i >= this.flags.length) {
                /*
                 * I found this can happen with some peculiar strings.
                 * Unfortunately the bug is not perfectly reproducible: I tried
                 * to launch the Tokenizer with the same input sentence (single-thread) and
                 * this bug does not appear every single time.
                 *
                 * Also it seems related both to string length and string composition:
                 * if I split the string in two it works, if I change the string with
                 * another one of the same length it works.
                 *
                 * You can find the string for testing in file "exploit_line.txt".
                 * Good luck!
                 */

                break;
            }

            this.flags[i] |= PROTECTED_FLAG;
        }
    }

    public void protect(int index) {
        this.protect(index, index + 1);
    }

    public Reader getReader() {
        return new CharArrayReader(chars, 0, length);
    }

    public String[] toTokenArray() {
        ArrayList<String> tokens = new ArrayList<>();

        int tokenStart = 0;
        int tokenEnd = 0;
        boolean foundNonWhitespace = false;

        for (int i = 0; i < length + 1; i++) {
            byte flag = this.flags[i];

            boolean protect = (flag & AnnotatedString.PROTECTED_FLAG) > 0;
            boolean split = (flag & AnnotatedString.SPLIT_FLAG) > 0;

            if (!protect && split) {
                int tokenLength = 1 + tokenEnd - tokenStart;

                if (tokenLength > 0) {
                    tokens.add(new String(chars, tokenStart, tokenLength));
                    tokenStart = tokenEnd = i;
                    foundNonWhitespace = false;
                }
            }

            if (i < chars.length) {
                if (chars[i] == ' ') {
                    if (!foundNonWhitespace)
                        tokenStart++;
                } else {
                    foundNonWhitespace = true;
                    tokenEnd = i;
                }
            }
        }

        return tokens.toArray(new String[tokens.size()]);
    }

    @Override
    public String toString() {
        return new String(chars, 0, length);
    }

}
