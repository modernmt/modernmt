package eu.modernmt.processing.tags;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.processing.AnnotatedString;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.regex.Matcher;

/**
 * Created by davide on 19/02/16.
 */
public class TagHighlighter implements TextProcessor<AnnotatedString, Sentence> {

    @Override
    public Sentence call(AnnotatedString astring) throws ProcessingException {
        int stringIndex = 0;
        char[] chars = astring.string.toCharArray();

        Matcher m = Tag.TagRegex.matcher(astring.string);

        ArrayList<Token> tokens = new ArrayList<>();
        ArrayList<Tag> tags = new ArrayList<>();

        while (m.find()) {
            int start = m.start();
            int end = m.end();

            if (stringIndex < start)
                extractTokens(chars, astring.bits, stringIndex, start, tokens);

            stringIndex = end;

            Tag tag = Tag.fromText(m.group());
            tag.setLeftSpace(start > 0 && chars[start - 1] == ' ');
            tag.setRightSpace(end < chars.length && chars[end] == ' ');
            tag.setPosition(tokens.size());
            tags.add(tag);
        }

        if (stringIndex < chars.length)
            extractTokens(chars, astring.bits, stringIndex, chars.length, tokens);

        return new Sentence(tokens.toArray(new Token[tokens.size()]), tags.toArray(new Tag[tags.size()]));
    }

    private static void extractTokens(char[] chars, BitSet bits, int start, int end, ArrayList<Token> output) {
        int length = chars.length;

        for (int i = start; i < end + 1; i++) {
            if (i == length || bits.get(i)) {
                String text = new String(chars, start, i - start).trim();

                if (!text.isEmpty()) {
                    Token token = new Token(new String(chars, start, i - start), true);
                    output.add(token);
                }

                start = i;
            }
        }
    }

    @Override
    public void close() throws IOException {
    }

}
