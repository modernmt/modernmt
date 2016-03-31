package eu.modernmt.processing.util;

import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.ProcessedString;
import eu.modernmt.processing.framework.string.StringEditor;

/**
 * Created by davide on 19/02/16.
 */
public class RareCharsNormalizer implements TextProcessor<ProcessedString, ProcessedString> {

    @Override
    public ProcessedString call(ProcessedString string) {
        char source[] = string.toCharArray();
        StringEditor editor = string.getEditor();

        for (int i = 0; i < source.length; i++) {
            char c = source[i];

            char nc = normalized(c);
            if (nc != '\0')
                editor.replace(i, 1, Character.toString(nc));
        }

        return editor.commitChanges();
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
