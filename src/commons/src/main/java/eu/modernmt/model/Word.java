package eu.modernmt.model;

/**
 * Created by davide on 17/02/16.
 */
public class Word extends Token {

    protected boolean rightSpaceRequired;

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


}
