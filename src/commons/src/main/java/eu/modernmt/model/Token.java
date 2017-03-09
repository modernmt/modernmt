package eu.modernmt.model;

import java.io.Serializable;

/**
 * Created by davide on 17/02/16.
 */
public class Token implements Serializable {

    /*the original text in the original string*/
    protected String text;
    /*the text version that was identified as a token:
    * It may be partly processed*/
    protected String placeholder;
    /*the original string between this token and the next one*/
    protected String rightSpace;

    public Token(String placeholder) {
        this(null, placeholder, null);
    }

    public Token(String placeholder, String rightSpace) {
        this(null, placeholder, rightSpace);
    }

    public Token(String text, String placeholder, String rightSpace) {
        this.text = text;
        this.placeholder = placeholder;
        this.rightSpace = rightSpace;
    }

    public String getText() {
        return text;
    }

    public boolean hasText() {
        return text != null;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getRightSpace() {
        return rightSpace;
    }

    public boolean hasRightSpace() {
        return rightSpace != null;
    }

    public void setRightSpace(String rightSpace) {
        if (rightSpace != null && rightSpace.isEmpty())
            rightSpace = null;

        this.rightSpace = rightSpace;
    }

    @Override
    public String toString() {
        return text == null ? placeholder : text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Token token = (Token) o;

        if (text != null ? !text.equals(token.text) : token.text != null) return false;
        if (!placeholder.equals(token.placeholder)) return false;
        return rightSpace != null ? rightSpace.equals(token.rightSpace) : token.rightSpace == null;

    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + placeholder.hashCode();
        result = 31 * result + (rightSpace != null ? rightSpace.hashCode() : 0);
        return result;
    }
}
