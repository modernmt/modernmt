package eu.modernmt.processing.util;

import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;

/**
 * Created by davide on 19/02/16.
 */
public class StringNormalizer implements TextProcessor<String, String> {

    private static final int WHITESPACE = 1;
    private static final int CONTROL = 2;

    @Override
    public String call(String param) throws ProcessingException {
        char source[] = param.toCharArray();
        char chars[] = new char[source.length];

        boolean start = true;
        boolean whitespace = false;

        int index = 0;

        for (char c : source) {
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
                        chars[index] = ' ';
                        whitespace = false;
                        index++;
                    }

                    chars[index] = c;

                    index++;
                    break;
            }
        }

        return new String(chars, 0, index);
    }

    @Override
    public void close() {
    }

}
