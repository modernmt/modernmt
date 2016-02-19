package eu.modernmt.processing.tags;

/**
 * Created by davide on 17/02/16.
 */
public class _Token {

    protected final String text;
    protected boolean rightSpace;

    public _Token(String text) {
        this(text, true);
    }

    public _Token(String text, boolean rightSpace) {
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
}
