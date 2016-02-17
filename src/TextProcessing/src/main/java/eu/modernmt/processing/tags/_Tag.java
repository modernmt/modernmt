package eu.modernmt.processing.tags;

/**
 * Created by davide on 17/02/16.
 */
public class _Tag extends _Token {

    protected boolean leftSpace;
    protected int position;

    public _Tag(String text) {
        this(text, true, true);
    }

    public _Tag(String text, boolean leftSpace, boolean rightSpace) {
        super(text, rightSpace);
        this.leftSpace = leftSpace;
    }

    public _Tag(String text, boolean leftSpace, boolean rightSpace, int position) {
        super(text, rightSpace);
        this.leftSpace = leftSpace;
        this.position = position;
    }

    public boolean hasLeftSpace() {
        return leftSpace;
    }

    public int getPosition() {
        return position;
    }

    public void setLeftSpace(boolean leftSpace) {
        this.leftSpace = leftSpace;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
