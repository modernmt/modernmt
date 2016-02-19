package eu.modernmt.model;

/**
 * Created by davide on 17/02/16.
 */
public class Token {

    protected final String text;
    protected boolean rightSpace;

    public Token(String text) {
        this(text, true);
    }

    public Token(String text, boolean rightSpace) {
        this.text = text;
        this.rightSpace = rightSpace;
    }

    public String getText() {
        return text;
    }

    public boolean hasRightSpace() {
        return rightSpace;
    }

    public void setRightSpace(boolean rightSpace) {
        this.rightSpace = rightSpace;
    }

    @Override
    public String toString() {
        return text;
    }
}
