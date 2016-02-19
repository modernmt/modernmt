package eu.modernmt.processing.tags;

import org.jetbrains.annotations.NotNull;

/**
 * Created by davide on 17/02/16.
 */


public class _Tag extends _Token implements Comparable<_Tag> {

    //note that position of
    protected Type type;

    protected String name; //name of the tag (without triangle brackets, backslash, and attributes
    protected boolean leftSpace;
    //position of the word after which the tag is placed; indexes of words start from 0
    // e.g. a tag at the beginning of the sentence has position=0
    // e.g. a tag at the end of the sentence (of Length words) has position=Length
    protected int position;

    public _Tag(String text) {
        this(text, true, true);
    }

    public _Tag(String text, boolean leftSpace, boolean rightSpace) {
        super(text, rightSpace);
        this.leftSpace = leftSpace;
    }

    public _Tag(String text, boolean leftSpace, boolean rightSpace, int position) {
        super(text, rightSpace);
        this.leftSpace = leftSpace;
        this.position = position;
        this.type = Type.UNDEF;
        this.name = "";
    }

    public _Tag(String text, boolean leftSpace, boolean rightSpace, int position, Type type) {
        super(text, rightSpace);
        this.leftSpace = leftSpace;
        this.position = position;
        this.type = type;
        this.name = "";
    }

    public _Tag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, String name) {
        super(text, rightSpace);
        this.leftSpace = leftSpace;
        this.position = position;
        this.type = type;
        this.name = name;
    }

    public static _Tag fromTag(_Tag fromTag) {
        return new _Tag(fromTag.getText(), fromTag.hasLeftSpace(), fromTag.hasRightSpace(), fromTag.getPosition(), fromTag.getType(), fromTag.getName());
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

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLeftSpace(boolean leftSpace) {
        this.leftSpace = leftSpace;
    }
    protected boolean isSelfContained() {
        return this.type == Type.SELF_CONTAINED;
    }
    protected boolean isOpening() {
        return this.type == Type.OPENED;
    }
    protected boolean isClosing() {
        return this.type == Type.CLOSED;
    }

    public int compareTo(@NotNull _Tag compareTag) {
        return Integer.compare(this.position, compareTag.getPosition());
    }

    protected enum Type {
        OPENED,        // ex: <a>
        CLOSED,        // ex: </a>
        CONTAINS_NONEMPTY_TEXT,     // ex: <a>data</a>
        CONTAINS_EMPTY_TEXT,        // ex: <a></a>
        OPENED_NONEMPTY_TEXT,     // ex: <a>data</a>
        CLOSED_NONEMPTY_TEXT,     // ex: <a>data</a>
        OPENED_EMPTY_TEXT,        // ex: <a></a>
        CLOSED_EMPTY_TEXT,        // ex: <a></a>
        SELF_CONTAINED,        // ex: <a/>
        // two additional types to handle broken markup:
        OPENED_BUT_UNCLOSED,        // ex: <a>
        CLOSED_BUT_UNOPENED,        // ex: </a>
        UNDEF      // still undefined
    }
}

