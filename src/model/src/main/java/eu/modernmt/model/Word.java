package eu.modernmt.model;

import java.io.Serializable;

/**
 * Created by davide on 17/02/16.
 */
public class Word extends Token {

    /**
     * @deprecated Transformation system is too complex to manage and extend
     * and it's not adequate for our purposes (i.e. NumericWord post-processor).
     */
    @Deprecated
    public interface Transformation extends Serializable {

        void apply(Word source, Word target);

    }

    protected int id;
    protected boolean oov;
    protected boolean rightSpaceRequired;
    protected Transformation transformation;

    public Word(int id) {
        super(null);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
        this.id = id;
        this.oov = false;
    }

    public Word(int id, String rightSpace) {
        super(null, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
        this.id = id;
        this.oov = false;
    }

    public Word(String placeholder) {
        super(placeholder);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
        this.id = 0;
        this.oov = false;
    }

    public Word(String placeholder, String rightSpace) {
        super(placeholder, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
        this.id = 0;
        this.oov = false;
    }

    public Word(String text, String placeholder, String rightSpace) {
        super(text, placeholder, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
        this.id = 0;
        this.oov = false;
    }

    public Word(String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
        super(text, placeholder, rightSpace);
        this.rightSpaceRequired = rightSpaceRequired;
        this.transformation = null;
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

    public Transformation getTransformation() {
        return transformation;
    }

    public void setTransformation(Transformation transformation) {
        this.transformation = transformation;
    }

    public final void applyTransformation(Word source) {
        if (this.transformation == null) {
            if (this.text == null) {
                if (source == null || !source.placeholder.equals(this.placeholder))
                    this.text = this.placeholder;
                else
                    this.text = source.text;
            }
        } else {
            this.transformation.apply(source, this);

            if (this.text == null)
                this.text = this.placeholder;
        }
    }

}
