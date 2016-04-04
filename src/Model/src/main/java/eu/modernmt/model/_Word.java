package eu.modernmt.model;

import java.io.Serializable;

/**
 * Created by davide on 17/02/16.
 */
public class _Word extends _Token {

    public interface Transformation extends Serializable {

        void apply(_Word source, _Word target);

    }

    protected boolean rightSpaceRequired;
    protected Transformation transformation;

    public _Word(String placeholder) {
        super(placeholder);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
    }

    public _Word(String placeholder, String rightSpace) {
        super(placeholder, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
    }

    public _Word(String text, String placeholder, String rightSpace) {
        super(text, placeholder, rightSpace);
        this.rightSpaceRequired = super.rightSpace != null;
        this.transformation = null;
    }

    public _Word(String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
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

    public final void applyTransformation(_Word source) {
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
