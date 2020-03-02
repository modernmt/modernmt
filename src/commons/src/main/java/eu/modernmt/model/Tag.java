package eu.modernmt.model;

abstract public class Tag extends Token implements Comparable<Tag>, Cloneable {

    public enum Type {
        OPENING_TAG,
        CLOSING_TAG,
        EMPTY_TAG,
    }

    protected Type type; /* tag type */
    protected final String name; /* tag name */
    /* position of the word after which the tag is placed; indexes of words start from 0
    e.g. a tag at the beginning of the sentence has position=0
    e.g. a tag at the end of the sentence (of Length words) has position=Length
    */
    protected int position;

    protected Tag(String name, String text, String leftSpace, String rightSpace, int position, Type type) {
        super(text, text, leftSpace, rightSpace);
        this.position = position;
        this.type = type;
        this.name = name;
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

    public boolean isEmptyTag() {
        return this.type == Type.EMPTY_TAG;
    }

    public boolean isOpeningTag() {
        return this.type == Type.OPENING_TAG;
    }

    public boolean isClosingTag() {
        return this.type == Type.CLOSING_TAG;
    }

    public boolean closes(Tag other) {
        return this.type == Type.CLOSING_TAG && other.type == Type.OPENING_TAG && nameEquals(this.name, other.name);
    }

    public boolean opens(Tag other) {
        return this.type == Type.OPENING_TAG && other.type == Type.CLOSING_TAG && nameEquals(this.name, other.name);
    }

    private static boolean nameEquals(String n1, String n2) {
        if (n1 == null)
            return n2 == null;
        else
            return n1.equals(n2);
    }

    @Override
    public int compareTo(Tag other) {
        return Integer.compare(this.position, other.getPosition());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Tag tag = (Tag) o;

        if (position != tag.position) return false;
        if (type != tag.type) return false;
        return name.equals(tag.name);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + position;
        return result;
    }

    @Override
    public abstract Tag clone();

}

