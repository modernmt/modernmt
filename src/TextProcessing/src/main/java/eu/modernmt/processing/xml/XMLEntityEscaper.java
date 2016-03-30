package eu.modernmt.processing.xml;

import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.ProcessedString;
import eu.modernmt.processing.framework.string.StringEditor;

/**
 * Created by davide on 29/03/16.
 */
public class XMLEntityEscaper implements TextProcessor<ProcessedString, ProcessedString> {

    @Override
    public ProcessedString call(ProcessedString string) {
        StringEditor editor = string.getEditor();
        char[] chars = string.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            String replacement = null;

            switch (c) {
                case '"':
                    replacement = "&quot;";
                    break;
                case '&':
                    replacement = "&amp;";
                    break;
                case '\'':
                    replacement = "&apos;";
                    break;
                case '<':
                    replacement = "&lt;";
                    break;
                case '>':
                    replacement = "&gt;";
                    break;
            }

            if (replacement != null)
                editor.replace(i, 1, replacement);
        }

        return editor.commitChanges();
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
