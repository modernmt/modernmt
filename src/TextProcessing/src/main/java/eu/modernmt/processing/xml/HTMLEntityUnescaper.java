package eu.modernmt.processing.xml;

import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.ProcessedString;
import eu.modernmt.processing.framework.string.StringEditor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by davide on 29/03/16.
 */
public class HTMLEntityUnescaper implements TextProcessor<ProcessedString, ProcessedString> {

    private static final Pattern EntityPattern = Pattern.compile("&((#[0-9]{1,4})|(#x[0-9a-fA-F]{1,4})|([a-zA-Z]+));");

    @Override
    public ProcessedString call(ProcessedString string) {
        StringEditor editor = string.getEditor();
        Matcher m = EntityPattern.matcher(string.toString());

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            String entity = m.group();
            Character c = XMLCharacterEntity.unescape(entity);
            if (c != null)
                editor.replace(start, end - start, c.toString());
        }

        return editor.commitChanges();
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
