package eu.modernmt.processing.tags;

import org.jetbrains.annotations.NotNull;

/**
 * Created by davide on 17/02/16.
 */


public class _Tag extends _Token implements Comparable<_Tag> {

    protected boolean leftSpace;
    //position of the word after which the tag is placed; indexes of words start from 0
    // e.g. a tag at the beginning of the sentence has position=0
    // e.g. a tag at the end of the sentence (of Length words) has position=Length
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

    public static _Tag fromTag(_Tag fromTag) {
        return new _Tag(fromTag.getText(), fromTag.hasLeftSpace(), fromTag.hasRightSpace(), fromTag.getPosition());
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

    public int compareTo(@NotNull _Tag compareTag) {
        return Integer.compare(this.position, compareTag.getPosition());

        /*        int comparePosition= (compareTag).getPosition();

        //ascending order
        return this.position - comparePosition;
*/
    }
}

