package eu.modernmt.model;

import java.util.regex.Pattern;

public class WhitespaceTag extends Tag {
    public static final Pattern TagRegex = Pattern.compile("[\t\n\r]+");

    private static final String NAME = "WhitespaceTag";

    public static WhitespaceTag fromText(String text) { return fromText(text, null, null, -1); }

    public static WhitespaceTag fromText(String text, String leftSpace, String rightSpace, int position) {
        return new WhitespaceTag(NAME, text, leftSpace, rightSpace, position, Type.EMPTY_TAG);
    }

    private WhitespaceTag(String name, String text, String leftSpace, String rightSpace, int position, Tag.Type type) {
        super(name, text, leftSpace, rightSpace, position, type);
    }

    @Override
    public String toString() {
        return text == null ? placeholder : text;
    }
}
