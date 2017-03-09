package eu.modernmt.model;

/**
 * Created by davide on 17/02/16.
 */
public class Word extends Token {

    protected int id;
    protected boolean oov;
    protected boolean rightSpaceRequired;

    public Word(int id) {
        super(null);
        this.rightSpaceRequired = super.rightSpace != null;
        this.id = id;
        this.oov = false;
    }

    public Word(int id, String rightSpace) {
        super(null, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
        this.id = id;
        this.oov = false;
    }

    public Word(String placeholder) {
        super(placeholder);
        this.rightSpaceRequired = super.rightSpace != null;
        this.id = 0;
        this.oov = false;
    }

    public Word(String placeholder, String rightSpace) {
        super(placeholder, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
        this.id = 0;
        this.oov = false;
    }

    public Word(String text, String placeholder, String rightSpace) {
        super(text, placeholder, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
        this.id = 0;
        this.oov = false;
    }

    public Word(String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
        super(text, placeholder, rightSpace);
        this.rightSpaceRequired = rightSpaceRequired;
        this.id = 0;
        this.oov = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isOutOfVocabulary() {
        return oov;
    }

    public void setOutOfVocabulary(boolean oov) {
        this.oov = oov;
    }

    public boolean isRightSpaceRequired() {
        return rightSpaceRequired;
    }

    public void setRightSpaceRequired(boolean rightSpaceRequired) {
        this.rightSpaceRequired = rightSpaceRequired;
    }

}
