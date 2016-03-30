package eu.modernmt.processing.xml;

import eu.modernmt.model.Tag;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.ProcessedString;
import eu.modernmt.processing.framework.string.StringEditor;

import java.util.regex.Matcher;

/**
 * Created by davide on 08/03/16.
 */
public class XMLTagExtractor implements TextProcessor<ProcessedString, ProcessedString> {

    @Override
    public ProcessedString call(ProcessedString string) {
        StringEditor editor = string.getEditor();
        Matcher m = Tag.TagRegex.matcher(string.toString());

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            editor.setXMLTag(start, end - start);
        }

        return editor.commitChanges();
    }

    @Override
    public void close() {
        // Nothing to do
    }

}
