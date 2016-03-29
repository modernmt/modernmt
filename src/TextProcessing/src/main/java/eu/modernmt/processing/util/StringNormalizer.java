package eu.modernmt.processing.util;

import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.ProcessedString;
import eu.modernmt.processing.framework.string.StringEditor;

/**
 * Created by davide on 19/02/16.
 */
public class StringNormalizer implements TextProcessor<ProcessedString, ProcessedString> {

    private static final int REGULAR = 0;
    private static final int WHITESPACE = 1;

    @Override
    public ProcessedString call(ProcessedString string) {
        char source[] = string.toCharArray();
        StringEditor editor = string.getEditor();

        boolean sentenceBegin = true;
        int whitespaceStart = -1;

        int i;
        for (i = 0; i < source.length; i++) {
            char c = source[i];
            int type = typeOf(c);

            switch (type) {
                case WHITESPACE:
                    if (whitespaceStart == -1)
                        whitespaceStart = i;
                    break;
                default:
                    if (whitespaceStart >= 0) {
                        editor.replace(whitespaceStart, i - whitespaceStart, sentenceBegin ? "" : " ");
                        whitespaceStart = -1;
                    }

                    sentenceBegin = false;

                    char nc = normalized(c);
                    if (nc != '\0')
                        editor.replace(i, 1, Character.toString(nc));

                    break;
            }
        }

        if (whitespaceStart >= 0)
            editor.replace(whitespaceStart, i - whitespaceStart, "");

        return editor.commitChanges();
    }

    private static int typeOf(char c) {
        if ((0x0009 <= c && c <= 0x000D) || c == 0x0020 || c == 0x00A0 || c == 0x1680 ||
                (0x2000 <= c && c <= 0x200A) || c == 0x202F || c == 0x205F || c == 0x3000) {
            return WHITESPACE;
        } else {
            return REGULAR;
        }
    }

    private static char normalized(char c) {
        char nc = '\0';

        switch (c) {
            case '`':
            case '‘':
            case '’':
                nc = '\'';
                break;
            case '«':
            case '»':
            case '“':
            case '”':
            case '„':
                nc = '"';
                break;
            case '–':
            case '—':
                nc = '-';
                break;
            default:
                break;
        }

        return nc;
    }

    @Override
    public void close() {
    }

}
