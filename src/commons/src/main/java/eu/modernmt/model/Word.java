package eu.modernmt.model;

import eu.modernmt.xml.XMLUtils;

/**
 * Created by davide on 17/02/16.
 */
public class Word extends Token {

    protected boolean rightSpaceRequired;
    private String xmlEscapedString = null;

    public Word(String placeholder) {
        super(placeholder);
        this.rightSpaceRequired = super.rightSpace != null;
    }

    public Word(String placeholder, String rightSpace) {
        super(placeholder, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
    }

    public Word(String text, String placeholder, String rightSpace) {
        super(text, placeholder, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
    }

    public Word(String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
        super(text, placeholder, rightSpace);
        this.rightSpaceRequired = rightSpaceRequired;
    }

    public boolean isRightSpaceRequired() {
        return rightSpaceRequired;
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
