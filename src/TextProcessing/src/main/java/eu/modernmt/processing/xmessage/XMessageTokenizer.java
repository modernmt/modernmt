package eu.modernmt.processing.xmessage;

import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.InvalidOperationException;
import eu.modernmt.processing.framework.string.XMLEditableString;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Created by davide on 08/04/16.
 */
public class XMessageTokenizer extends TextProcessor<XMLEditableString, XMLEditableString> {

    public XMessageTokenizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public XMLEditableString call(XMLEditableString string, Map<String, Object> metadata) throws InvalidOperationException {
        XMLEditableString.Editor editor = string.getEditor();

        Matcher m = XFormat.PLACEHOLDER_PATTERN.matcher(string.toString());

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            editor.setWord(start, end - start);
        }

        return editor.commitChanges();
    }

}
