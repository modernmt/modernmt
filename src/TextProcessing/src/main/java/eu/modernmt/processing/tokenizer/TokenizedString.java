package eu.modernmt.processing.tokenizer;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;

/**
 * Created by davide on 19/02/16.
 */
public class TokenizedString {

    public final String string;
    private final BitSet bits;
    private final int length;
    private HashSet<Integer> tagIndexes = new HashSet<>();

    public TokenizedString(String string) {
        this(string, new BitSet(string.length() + 1));
    }

    public TokenizedString(String string, BitSet bits) {
        this.string = string;
        this.bits = bits;
        this.length = string.length();
    }

    public void setToken(int start, int end) {
        if (start < length) {
            if (end > length)
                end = length;

            bits.set(start);
            bits.set(start + 1, end, false);
            bits.set(end);
        }
    }

    public void setTag(int start, int end) {
        setToken(start, end);
        tagIndexes.add(start);
    }

    public String toDebugString() {
        StringBuilder builder = new StringBuilder();

        char[] chars = string.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (bits.get(i))
                builder.append('|');

            builder.append(chars[i]);
        }

        return builder.toString();
    }

    public Sentence toSentence() {
        char[] chars = string.toCharArray();

        ArrayList<Token> tokens = new ArrayList<>();
        ArrayList<Tag> tags = new ArrayList<>();

        int start = 0;

        for (int i = 0; i < chars.length + 1; i++) {
            if (i == chars.length || bits.get(i)) {
                // Left trimming
                for (; start < i; start++) {
                    if (chars[start] != ' ')
                        break;
                }

                // Right trimming
                int j = i;
                for (; j > start; j--) {
                    if (chars[j - 1] != ' ')
                        break;
                }

                if (start == j) {
                    continue;
                }

                String text = new String(chars, start, j - start);

                if (tagIndexes.contains(start)) {
                    Tag tag = Tag.fromText(text);
                    tag.setLeftSpace(start > 0 && chars[start - 1] == ' ');
                    tag.setRightSpace(j < chars.length && chars[j] == ' ');
                    tag.setPosition(tokens.size());
                    tags.add(tag);
                } else {
                    boolean rightSpace = j < chars.length && chars[j] == ' ';
                    Token token = new Token(text, rightSpace);
                    tokens.add(token);
                }

                start = i;
            }
        }

        return new Sentence(tokens.toArray(new Token[tokens.size()]), tags.toArray(new Tag[tags.size()]));
    }

    public int length() {
        return length;
    }

    @Override
    public String toString() {
        return string;
    }

}
