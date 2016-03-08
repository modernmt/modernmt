package eu.modernmt.processing.xml;

import eu.modernmt.model.Tag;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.tokenizer.TokenizedString;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by davide on 08/03/16.
 */
public class XMLStringParser implements TextProcessor<String, TokenizedString> {

    private static final Pattern EntityPattern = Pattern.compile("&((#[0-9]{1,4})|(#x[0-9a-fA-F]{1,4})|([a-zA-Z]+));");

    public static final class XMLTagHook {

        public final Tag tag;
        public final int position;

        public XMLTagHook(Tag tag, int position) {
            this.tag = tag;
            this.position = position;
        }

    }

    @Override
    public TokenizedString call(String string) throws ProcessingException {
        StringBuilder sequence = new StringBuilder();
        ArrayList<XMLTagHook> tags = new ArrayList<>();

        Matcher m = Tag.TagRegex.matcher(string);

        int stringIndex = 0;
        char[] chars = string.toCharArray();

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            if (stringIndex < start)
                normalize(sequence, chars, stringIndex, start);

            stringIndex = end;

            Tag tag = Tag.fromText(m.group());
            tag.setLeftSpace(start > 0 && chars[start - 1] == ' ');
            tag.setRightSpace(end < chars.length && chars[end] == ' ');
            tags.add(new XMLTagHook(tag, sequence.length()));

            sequence.append(' ');
        }

        if (stringIndex < chars.length)
            normalize(sequence, chars, stringIndex, chars.length);

        System.out.println(sequence);
        for (XMLTagHook hook : tags) {
            System.out.println(hook.tag + " #" + hook.position);
        }

        return null;
    }

    private static void normalize(StringBuilder output, char[] chars, int start, int end) {
        if (chars[end - 1] == ' ')
            end--;

        CharSequence sequence = CharBuffer.wrap(chars, start, end - start);
        Matcher m = EntityPattern.matcher(sequence);

        int stringIndex = start;

        while (m.find()) {
            int mstart = m.start() + start;
            int mend = m.end() + start;

            if (stringIndex < mstart)
                output.append(chars, stringIndex, mstart - stringIndex);

            String entity = m.group();
            Character c = XMLCharacterEntity.unescape(entity);
            if (c == null) {
                output.append(entity);
            } else {
                output.append(c);
            }

            stringIndex = mend;
        }

        if (stringIndex < end)
            output.append(chars, stringIndex, end - stringIndex);
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }

    public static void main(String[] args) throws ProcessingException {
        String s = "<a>ciao &apos;</a>. &#182; &#xBC;<button> &lt;button&gt;";
        new XMLStringParser().call(s);
    }

}
