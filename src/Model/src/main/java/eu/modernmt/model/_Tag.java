package eu.modernmt.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class _Tag extends _Token implements Comparable<_Tag> {

    private static final String TAG_NAME = "(\\p{Alpha}|_|:)(\\p{Alpha}|\\p{Digit}|\\.|-|_|:|)*";

    private static final Pattern TagNameRegex = Pattern.compile(TAG_NAME);
    public static final Pattern TagRegex = Pattern.compile(
            "(<(" + TAG_NAME + ")[^>]*/?>)|" +
                    "(<!(" + TAG_NAME + ")[^>]*[^/]>)|" +
                    "(</(" + TAG_NAME + ")[^>]*>)|" +
                    "(<!--)|(-->)");

    public enum Type {
        OPENING_TAG,
        CLOSING_TAG,
        EMPTY_TAG,
    }

    public static _Tag fromText(String text) {
        return fromText(text, false, null, -1);
    }

    public static _Tag fromText(String text, boolean leftSpace, String rightSpace, int position) {
        if ("<!--".equals(text)) {
            return new _Tag("--", text, leftSpace, rightSpace, position, Type.OPENING_TAG, false);
        } else if ("-->".equals(text)) {
            return new _Tag("--", text, leftSpace, rightSpace, position, Type.CLOSING_TAG, false);
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

        return new _Tag(name, text, leftSpace, rightSpace, position, type, dtd);
    }

    public static _Tag fromTag(_Tag other) {
        return new _Tag(other.name, other.text, other.leftSpace, other.rightSpace, other.position, other.type, other.dtd);
    }

    protected final Type type; /* tag type */
    protected final String name; /* tag name */
    protected boolean leftSpace; /* true if there is at least one space on the left of the tag*/
    /* position of the word after which the tag is placed; indexes of words start from 0
    e.g. a tag at the beginning of the sentence has position=0
    e.g. a tag at the end of the sentence (of Length words) has position=Length
    */
    protected int position;
    protected boolean dtd;

    protected _Tag(String name, String text, boolean leftSpace, String rightSpace, int position, Type type, boolean dtd) {
        super(text, text, rightSpace);
        this.leftSpace = leftSpace;
        this.position = position;
        this.type = type;
        this.name = name;
        this.dtd = dtd;
    }

    public boolean hasLeftSpace() {
        return leftSpace;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setLeftSpace(boolean leftSpace) {
        this.leftSpace = leftSpace;
    }

    public boolean isEmptyTag() {
        return this.type == Type.EMPTY_TAG;
    }

    public boolean isOpeningTag() {
        return this.type == Type.OPENING_TAG;
    }

    public boolean isClosingTag() {
        return this.type == Type.CLOSING_TAG;
    }

    public boolean isDTD() {
        return dtd;
    }

    public boolean isComment() {
        return "--".equals(name);
    }

    public boolean closes(_Tag other) {
        return !this.dtd && this.type == Type.CLOSING_TAG && other.type == Type.OPENING_TAG && nameEquals(this.name, other.name);
    }

    public boolean opens(_Tag other) {
        return !this.dtd && this.type == Type.OPENING_TAG && other.type == Type.CLOSING_TAG && nameEquals(this.name, other.name);
    }

    private static boolean nameEquals(String n1, String n2) {
        if (n1 == null)
            return n2 == null;
        else
            return n1.equals(n2);
    }

    @Override
    public int compareTo(_Tag other) {
        return Integer.compare(this.position, other.getPosition());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        _Tag tag = (_Tag) o;

        if (leftSpace != tag.leftSpace) return false;
        if (position != tag.position) return false;
        if (dtd != tag.dtd) return false;
        if (type != tag.type) return false;
        return name.equals(tag.name);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (leftSpace ? 1 : 0);
        result = 31 * result + position;
        result = 31 * result + (dtd ? 1 : 0);
        return result;
    }
}

