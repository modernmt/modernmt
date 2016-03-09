package eu.modernmt.processing.tokenizer;

import eu.modernmt.model.Tag;

import java.util.BitSet;

/**
 * Created by davide on 19/02/16.
 */
public class TokenizedString {

    public static final class XMLTagHook {

        public final Tag tag;
        public final int position;

        public XMLTagHook(Tag tag, int position) {
            this.tag = tag;
            this.position = position;
        }

    }

    public final String string;
    public final BitSet bits;
    private final int length;
    public final XMLTagHook[] hooks;

    public TokenizedString(String string) {
        this(string, null);
    }

    public TokenizedString(String string, XMLTagHook[] hooks) {
        this.string = string;
        this.hooks = hooks == null ? new XMLTagHook[0] : hooks;
        this.length = string.length();
        this.bits = new BitSet(string.length() + 1);
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

    public int length() {
        return length;
    }

    @Override
    public String toString() {
        return string;
    }

}
