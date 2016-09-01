package eu.modernmt.processing.xml;

import eu.modernmt.model.Tag;
import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.XMLEditableString;

import java.nio.CharBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by davide on 08/03/16.
 */
public class XMLStringBuilder extends TextProcessor<String, XMLEditableString> {

    private static final Pattern EntityPattern = Pattern.compile("&((#[0-9]{1,4})|(#x[0-9a-fA-F]{1,4})|([a-zA-Z]+));");

    public XMLStringBuilder(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public XMLEditableString call(String source, Map<String, Object> metadata) throws ProcessingException {
        XMLEditableString.Builder builder = new XMLEditableString.Builder();
        Matcher m = Tag.TagRegex.matcher(source);

        int stringIndex = 0;
        char[] chars = source.toCharArray();

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            if (stringIndex < start)
                escapeAndAppend(builder, chars, stringIndex, start);

            stringIndex = end;

            builder.appendXMLTag(m.group());
        }

        if (stringIndex < chars.length)
            escapeAndAppend(builder, chars, stringIndex, chars.length);

        return builder.create();
    }

    private static void escapeAndAppend(XMLEditableString.Builder builder, char[] chars, int start, int end) {
        CharSequence sequence = CharBuffer.wrap(chars, start, end - start);
        Matcher m = EntityPattern.matcher(sequence);

        int stringIndex = start;

        while (m.find()) {
            int mstart = m.start() + start;
            int mend = m.end() + start;

            if (stringIndex < mstart)
                builder.append(chars, stringIndex, mstart - stringIndex);

            String entity = m.group();
            Character c = XMLCharacterEntity.unescape(entity);
            if (c == null) {
                builder.append(entity);
            } else {
                builder.append(c);
            }

            stringIndex = mend;
        }

        if (stringIndex < end)
            builder.append(chars, stringIndex, end - stringIndex);
    }

}
