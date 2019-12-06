package eu.modernmt.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class XMLTag extends Tag implements Comparable<Tag> {

    private static final String XML_TAG_NAME = "(\\p{Alpha}|_|:)(\\p{Alpha}|\\p{Digit}|\\.|-|_|:|)*";

    private static final Pattern TagNameRegex = Pattern.compile(XML_TAG_NAME);
    public static final Pattern TagRegex = Pattern.compile(
            "(<(" + XML_TAG_NAME + ")[^>]*/?>)|" +
                    "(<!(" + XML_TAG_NAME + ")[^>]*[^/]>)|" +
                    "(</(" + XML_TAG_NAME + ")[^>]*>)|" +
                    "(<!--)|(-->)");

    public static XMLTag fromText(String text) {
        return fromText(text, null, null, -1);
    }

    public static XMLTag fromText(String text, String leftSpace, String rightSpace, int position) {
        if ("<!--".equals(text)) {
            return new XMLTag("--", text, leftSpace, rightSpace, position, Type.OPENING_TAG, false);
        } else if ("-->".equals(text)) {
            return new XMLTag("--", text, leftSpace, rightSpace, position, Type.CLOSING_TAG, false);
        }

        int length = text.length();

        if (length < 3)
            throw new IllegalArgumentException("Invalid tag: " + text);

        String name;
        Type type;
        boolean dtd = false;
        int nameStartPosition = 1;

        if (text.charAt(1) == '!') {
            dtd = true;
            type = Type.OPENING_TAG;
            nameStartPosition = 2;
        } else if (text.charAt(1) == '/') {
            type = Type.CLOSING_TAG;
            nameStartPosition = 2;
        } else if (text.charAt(length - 2) == '/') {
            type = Type.EMPTY_TAG;
        } else {
            type = Type.OPENING_TAG;
        }

        Matcher matcher = TagNameRegex.matcher(text);
        if (!matcher.find(nameStartPosition))
            throw new IllegalArgumentException("Invalid tag: " + text);
        name = matcher.group();

        return new XMLTag(name, text, leftSpace, rightSpace, position, type, dtd);
    }

    public static XMLTag fromTag(XMLTag other) {
        return new XMLTag(other.name, other.text, other.leftSpace, other.rightSpace, other.position, other.type, other.dtd);
    }

    private boolean dtd;

    private XMLTag(String name, String text, String leftSpace, String rightSpace, int position, Type type, boolean dtd) {
        super(name, text, leftSpace, rightSpace, position, type);
        this.dtd = dtd;
    }

    public boolean isDTD() {
        return dtd;
    }

    public boolean isComment() {
        return "--".equals(name);
    }

    @Override
    public boolean closes(Tag other) {
        return !this.dtd && super.closes(other);
    }

    @Override
    public boolean opens(Tag other) {
        return !this.dtd && super.opens(other);
    }

    @Override
    public boolean equals(Object o) {
        XMLTag tag = (XMLTag) o;
        if (dtd != tag.dtd) return false;
        return super.equals(o);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (dtd ? 1 : 0);
        return result;
    }
}

