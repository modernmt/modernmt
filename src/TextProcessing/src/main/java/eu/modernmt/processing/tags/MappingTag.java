package eu.modernmt.processing.tags;

import eu.modernmt.model.Tag;

import java.util.ArrayList;

/**
 * Created by nicolabertoldi on 18/02/16.
 */
public class MappingTag extends Tag {

    protected Tag link; //pointer to the closing (or opening) tag; null if undefined (SELF_CONTAINED, OPENED_BUT_UNCLOSED, CLOSED_BUT_UNOPENED)
    protected int parent;
    protected ArrayList<Integer> coveredPositions; //list of the word positions covered by the tag

    public MappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, String name) {
        super(name, text, leftSpace, rightSpace, position, type);
        this.link = null;
        this.parent = -1;
        this.coveredPositions = new ArrayList<>();
    }

    public MappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, String name, Tag link) {
        super(name, text, leftSpace, rightSpace, position, type);
        this.link = link;
        this.parent = -1;
        this.coveredPositions = new ArrayList<>();
    }

    public MappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, String name, Tag link, int parent) {
        super(name, text, leftSpace, rightSpace, position, type);
        this.link = link;
        this.parent = parent;
        this.coveredPositions = new ArrayList<>();
    }


    public static MappingTag fromTag(Tag fromTag) {
        return new MappingTag(fromTag.getText(), fromTag.hasLeftSpace(), fromTag.hasRightSpace(), fromTag.getPosition(), fromTag.getType(), fromTag.getName());
    }

    public static MappingTag fromTag(MappingTag fromTag) {
        return new MappingTag(fromTag.getText(), fromTag.hasLeftSpace(), fromTag.hasRightSpace(), fromTag.getPosition(), fromTag.getType(), fromTag.getName(), fromTag.getLink(), fromTag.getParent());
    }

    public Tag getLink() {
        return link;
    }

    public int getParent() {
        return parent;
    }

    public ArrayList<Integer> getCoveredPositions() {
        return coveredPositions;
    }

    public void setLink(Tag link) {
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
        return this.isOpeningTag() && !this.hasLink();
    }

    protected boolean isClosedButUnopend() {
        return this.isClosingTag() && !this.hasLink();
    }

    protected boolean isOpenedEmpty() {
        return this.isOpeningTag() && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) == 0;
    }

    protected boolean isOpenedNonEmpty() {
        return this.isOpeningTag() && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) > 0;
    }

    protected boolean isClosedEmpty() {
        return this.isClosingTag() && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) == 0;
    }

    protected boolean isClosedNonEmpty() {
        return this.isClosingTag() && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) > 0;
    }

    protected boolean isEmptyTag() {
        return this.type == Type.EMPTY_TAG;
    }

    protected boolean isOpeningTag() {
        return this.type == Type.OPENING_TAG;
    }

    protected boolean isClosingTag() {
        return this.type == Type.CLOSING_TAG;
    }

}
