package eu.modernmt.model;

import java.io.Serializable;

/**
 * Created by davide on 17/02/16.
 */
public class Token implements Serializable {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Token token = (Token) o;

        if (rightSpace != token.rightSpace) return false;
        return text.equals(token.text);

    }

    @Override
    public int hashCode() {
        int result = text.hashCode();
        result = 31 * result + (rightSpace ? 1 : 0);
        return result;
    }
}
