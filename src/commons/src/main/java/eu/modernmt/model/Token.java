package eu.modernmt.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by davide on 17/02/16.
 */
public class Token implements Serializable {

    // the original form of this token
    protected String text;
    // the text version that was identified as a token, it may be partly processed
    protected String placeholder;
    // the original string between this token and the previous one
    protected String leftSpace;
    // the original string between this token and the next one
    protected String rightSpace;

    // if true, this token mark an end of sentence
    private boolean sentenceBreak;


    public Token(String text, String placeholder, String leftSpace, String rightSpace) {
        this.text = text;
        this.placeholder = placeholder;
        this.leftSpace = leftSpace;
        this.rightSpace = rightSpace;

        this.sentenceBreak = false;
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

    public String getLeftSpace() {
        return leftSpace;
    }

    public String getRightSpace() {
        return rightSpace;
    }

    public boolean hasLeftSpace() {
        return leftSpace != null;
    }

    public boolean hasRightSpace() {
        return rightSpace != null;
    }

    public void setLeftSpace(String space) {
        this.leftSpace = (space != null && space.isEmpty()) ? null : space;
    }

    public void setRightSpace(String space) {
        this.rightSpace = (space != null && space.isEmpty()) ? null : space;
    }

    public Token(String placeholder) {
        this(null, placeholder, null,null);
    }

    public Token(String placeholder, String leftSpace, String rightSpace) {
        this(null, placeholder, leftSpace, rightSpace);
    }

    public boolean isSentenceBreak() {
        return sentenceBreak;
    }

    public void setSentenceBreak(boolean sentenceBreak) {
        this.sentenceBreak = sentenceBreak;
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

        if (!Objects.equals(text, token.text)) return false;
        if (!placeholder.equals(token.placeholder)) return false;
        return Objects.equals(leftSpace, token.leftSpace) && Objects.equals(rightSpace, token.rightSpace);

    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + placeholder.hashCode();
        result = 31 * result + (leftSpace != null ? leftSpace.hashCode() : 0);
        result = 31 * result + (rightSpace != null ? rightSpace.hashCode() : 0);
        return result;
    }
}
