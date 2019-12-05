package eu.modernmt.model;

import eu.modernmt.xml.XMLUtils;

/**
 * Created by davide on 17/02/16.
 */
public class Word extends Token {

    protected boolean rightSpaceRequired;
    protected boolean leftSpaceRequired;
    private String xmlEscapedString = null;

    public Word(String placeholder) {
        this(null, placeholder, null,null);
    }

    //TODO: remove after fixing UnitTests
    public Word(String placeholder, String rightSpace) {
        this(null, placeholder, null, rightSpace);
    }

    public Word(String text, String placeholder, String rightSpace) {
        this(text, placeholder, null, rightSpace);
    }

    public Word(String text, String placeholder, String leftSpace, String rightSpace) {
        this(text, placeholder, leftSpace, rightSpace, leftSpace != null, rightSpace != null);
    }

    //TODO: remove after fixing UnitTests
    public Word(String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
        this(text, placeholder,null, rightSpace, false, rightSpaceRequired);
    }

    public Word(String text, String placeholder, String leftSpace, String rightSpace, boolean leftSpaceRequired, boolean rightSpaceRequired) {
        super(text, placeholder, leftSpace, rightSpace);
        this.leftSpaceRequired = leftSpaceRequired;
        this.rightSpaceRequired = rightSpaceRequired;
    }


    public boolean isLeftSpaceRequired() {
        return leftSpaceRequired;
    }

    public boolean isRightSpaceRequired() {
        return rightSpaceRequired;
    }

    public void setLeftSpaceRequired(boolean leftSpaceRequired) {
        this.leftSpaceRequired = leftSpaceRequired;
    }

    public void setRightSpaceRequired(boolean rightSpaceRequired) {
        this.rightSpaceRequired = rightSpaceRequired;
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        xmlEscapedString = null;
    }

    @Override
    public void setPlaceholder(String placeholder) {
        super.setPlaceholder(placeholder);
        xmlEscapedString = null;
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean escape) {
        if (escape) {
            if (xmlEscapedString == null)
                xmlEscapedString = XMLUtils.escapeText(super.toString());

            return xmlEscapedString;
        } else {
            return super.toString();
        }
    }

}
