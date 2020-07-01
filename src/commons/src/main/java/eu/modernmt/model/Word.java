package eu.modernmt.model;

import eu.modernmt.xml.XMLUtils;

/**
 * Created by davide on 17/02/16.
 */
public class Word extends Token {

    //true if a space is required between the previous Word and this Word, according to the (de)tokenization rules
    private boolean leftSpaceRequired;
    //true if a space is required between this Word and the next Word, according to the (de)tokenization rules
    private boolean rightSpaceRequired;
    private String xmlEscapedString = null;


    // the attribute hasHiddenLeftSpace is:
    // - true, if between this token and the closest left Word should be present a space (according to the (de)tokenization modules)),
    //        but the space is not actually present because there is a Token in-between
    // - false, otherwise
    // The attribute is set during the creation of the source sentence, and it is projected from the source to the target by the WhiteSpaceProjector
    // The attribute is needed for having the right spaces:
    // - when the sentence is printed without tags (XMLStripped)
    // - when spaces are projected to the target sentence; Remind that tags and words are re-aligned
    private boolean hasHiddenLeftSpace;

    // the attribute hasHiddenLeftSpace is:
    // - true, if between this token and the closest left Word should be present a space (according to the (de)tokenization modules)),
    //        but the space is not actually present because there is a Token in-between
    // - false, otherwise
    // The attribute is set during the creation of the source sentence, and it is projected from the source to the target by the WhiteSpaceProjector
    // The attribute is needed for having the right spaces:
    // - when the sentence is printed without tags (XMLStripped)
    // - when spaces are projected to the target sentence; Remind that tags and words are re-aligned
    private boolean hasHiddenRightSpace;

    public boolean hasHiddenLeftSpace() {
        return hasHiddenLeftSpace;
    }

    public boolean hasHiddenRightSpace() {
        return hasHiddenRightSpace;
    }

    public void setHiddenLeftSpace(boolean hasHiddenLeftSpace) {
        this.hasHiddenLeftSpace = hasHiddenLeftSpace;
    }

    public void setHiddenRightSpace(boolean hasHiddenRightSpace) {
        this.hasHiddenRightSpace = hasHiddenRightSpace;
    }


    public Word(String placeholder) {
        this(null, placeholder, null,null);
    }

    public Word(String placeholder,  String leftSpace, String rightSpace) {
        this(null, placeholder, leftSpace, rightSpace);
    }

    public Word(String text, String placeholder, String leftSpace, String rightSpace) {
        this(text, placeholder, leftSpace, rightSpace, leftSpace != null, rightSpace != null);
    }

    public Word(String placeholder, String leftSpace, String rightSpace, boolean leftSpaceRequired, boolean rightSpaceRequired) {
        this(null, placeholder, leftSpace, rightSpace,leftSpaceRequired, rightSpaceRequired);
    }

    public Word(String text, String placeholder, String leftSpace, String rightSpace, boolean leftSpaceRequired, boolean rightSpaceRequired) {
        super(text, placeholder, leftSpace, rightSpace);
        this.leftSpaceRequired = leftSpaceRequired;
        this.rightSpaceRequired = rightSpaceRequired;

        this.hasHiddenLeftSpace = false;
        this.hasHiddenRightSpace = false;
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
