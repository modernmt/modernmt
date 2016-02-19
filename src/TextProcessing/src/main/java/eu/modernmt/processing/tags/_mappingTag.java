package eu.modernmt.processing.tags;

import java.util.ArrayList;

/**
 * Created by nicolabertoldi on 18/02/16.
 */
public class _mappingTag extends _Tag{

    protected _Tag link; //pointer to the closing (or opening) tag; null if undefined (SELF_CONTAINED, OPENED_BUT_UNCLOSED, CLOSED_BUT_UNOPENED)
    protected int parent;
    protected ArrayList<Integer> coveredPositions; //list of the word positions covered by the tag


    public _mappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, String name) {
        super(text, rightSpace, leftSpace, position, type, name);
        this.link = null;
        this.parent = -1;
        this.coveredPositions = new ArrayList<>();
    }

    public _mappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, String name, _Tag link) {
        super(text, rightSpace, leftSpace, position, type, name);
        this.link = link;
        this.parent = -1;
        this.coveredPositions = new ArrayList<>();
    }

    public _mappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, String name, _Tag link, int parent) {
        super(text, rightSpace, leftSpace, position, type, name);
        this.link = link;
        this.parent = parent;
        this.coveredPositions = new ArrayList<>();
    }


    public static _mappingTag fromTag(_Tag fromTag) {
        return new _mappingTag(fromTag.getText(), fromTag.hasLeftSpace(), fromTag.hasRightSpace(), fromTag.getPosition(), fromTag.getType(), fromTag.getName());
    }

    public static _mappingTag fromTag(_mappingTag fromTag) {
        return new _mappingTag(fromTag.getText(), fromTag.hasLeftSpace(), fromTag.hasRightSpace(), fromTag.getPosition(), fromTag.getType(), fromTag.getName(), fromTag.getLink(), fromTag.getParent());
    }

    public _Tag getLink() { return link; }
    public int getParent() { return parent; }
    public ArrayList<Integer> getCoveredPositions() { return coveredPositions; }

    public void setLink(_Tag link) {
        this.link = link;
    }
    public void setParent(int parent) {
        this.parent = parent;
    }
    public void setCoveredPositions(ArrayList<Integer> coveredPositions) {
        this.coveredPositions.addAll(coveredPositions);
    }

    protected boolean hasLink() {
        return this.link != null;
    }
    protected boolean isOpenedButUncloed() {
        return this.isOpening() && !this.hasLink();
    }
    protected boolean isClosedButUnopend() {
        return this.isClosing() && !this.hasLink();
    }
    protected boolean isOpenedEmpty() {
        return this.isOpening()  && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) == 0;
    }
    protected boolean isOpenedNonEmpty() {
        return this.isOpening()  && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) > 0;
    }
    protected boolean isClosedEmpty() {
        return this.isClosing() && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) == 0;
    }
    protected boolean isClosedNonEmpty() {
        return this.isClosing() && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) > 0;
    }
}
