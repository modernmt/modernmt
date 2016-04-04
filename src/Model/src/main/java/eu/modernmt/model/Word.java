package eu.modernmt.model;

import java.io.Serializable;

/**
 * Created by davide on 17/02/16.
 */
public class Word extends Token {

    public interface Transformation extends Serializable {

        void apply(Word source, Word target);

    }

    protected boolean rightSpaceRequired;
    protected Transformation transformation;

    public Word(String placeholder) {
        super(placeholder);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
    }

    public Word(String placeholder, String rightSpace) {
        super(placeholder, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
    }

    public Word(String text, String placeholder, String rightSpace) {
        super(text, placeholder, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
    }

    public Word(String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
        super(text, placeholder, rightSpace);
        this.rightSpaceRequired = rightSpaceRequired;
        this.transformation = null;
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
            if (source == null || !source.placeholder.equals(this.placeholder))
                this.text = this.placeholder;
            else
                this.text = source.text;
        } else {
            this.transformation.apply(source, this);
        }
    }

}
