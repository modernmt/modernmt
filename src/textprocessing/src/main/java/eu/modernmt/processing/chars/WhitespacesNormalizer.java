package eu.modernmt.processing.chars;

import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.XMLEditableString;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 19/02/16.
 */
public class WhitespacesNormalizer extends TextProcessor<XMLEditableString, XMLEditableString> {

    public WhitespacesNormalizer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public XMLEditableString call(XMLEditableString string, Map<String, Object> metadata) throws ProcessingException {
        char source[] = string.toCharArray();
        XMLEditableString.Editor editor = string.getEditor();

        boolean sentenceBegin = true;
        int whitespaceStart = -1;

        int i;
        for (i = 0; i < source.length; i++) {
            char c = source[i];

            if (isWhitespace(c)) {
                if (whitespaceStart == -1)
                    whitespaceStart = i;
            } else {
                if (whitespaceStart >= 0) {
                    if (sentenceBegin)
                        editor.delete(whitespaceStart, i - whitespaceStart);
                    else
                        editor.replace(whitespaceStart, i - whitespaceStart, " ");
                    whitespaceStart = -1;
                }

                sentenceBegin = false;
            }
        }

        if (whitespaceStart >= 0)
            editor.delete(whitespaceStart, i - whitespaceStart);

        return editor.commitChanges();
    }

    public static boolean isWhitespace(char c) {
        return ((0x0009 <= c && c <= 0x000D) || c == 0x0020 || c == 0x00A0 || c == 0x1680 ||
                (0x2000 <= c && c <= 0x200A) || c == 0x202F || c == 0x205F || c == 0x3000);
    }

}
