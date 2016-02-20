package eu.modernmt.processing.tags;

import eu.modernmt.model.Tag;

import java.util.ArrayList;

/**
 * Created by nicolabertoldi on 18/02/16.
 */
public class MappingTag extends Tag {


    /* pointer to the corresponding closing (or opening) tag; null if not present or the tag an EMPTY_TAG */
    protected MappingTag link;
    /* list of the word positions covered by the tag */
    protected ArrayList<Integer> coveredPositions;
    /* true if the tag covers at least one position */
    protected boolean content;

    public MappingTag(String name, String text, boolean leftSpace, boolean rightSpace, int position, Type type) {
        super(name, text, leftSpace, rightSpace, position, type);
        this.link = null;
        this.content = false;
        this.coveredPositions = new ArrayList<>();
    }
/*
    public MappingTag(String name, String text, boolean leftSpace, boolean rightSpace, int position, Type type, MappingTag link) {
        super(name, text, leftSpace, rightSpace, position, type);
        this.link = link;
        this.content = false;
        this.coveredPositions = new ArrayList<>();
    }
*/

    public MappingTag(String name, String text, boolean leftSpace, boolean rightSpace, int position, Type type, MappingTag link, boolean content) {
        super(name, text, leftSpace, rightSpace, position, type);
        this.link = link;
        this.content = content;
        this.coveredPositions = new ArrayList<>();
    }

    public static MappingTag fromTag(Tag fromTag) {
        return new MappingTag(fromTag.getName(), fromTag.getText(), fromTag.hasLeftSpace(), fromTag.hasRightSpace(), fromTag.getPosition(), fromTag.getType());
    }

    @Override
    public MappingTag clone() {
        return new MappingTag(name, text, leftSpace, rightSpace, position, type, link, content);
    }

    public MappingTag getLink() {
        return link;
    }

    public boolean getContent() {
        return content;
    }

    public ArrayList<Integer> getCoveredPositions() {
        return coveredPositions;
    }

    public void setLink(MappingTag link) {
        this.link = link;
    }

    public void setContent(boolean content) {
        this.content = content;
    }

    public void setCoveredPositions(ArrayList<Integer> coveredPositions) {
        this.coveredPositions.clear();
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

    /* true if is an OPENING_TAG and there is not any word between the opening and closing tags */
    protected boolean isOpenedEmpty() {
        return this.isOpeningTag() && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) == 0;
    }

    /* true if is an OPENING_TAG and there is at least one word between the opening and closing tags */
    protected boolean isOpenedNonEmpty() {
        return this.isOpeningTag() && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) > 0;
    }

    /* true if is an CLOSING_TAG and there is not any word between the opening and closing tags */
    protected boolean isClosedEmpty() {
        return this.isClosingTag() && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) == 0;
    }

    /* true if is an CLOSING_TAG and there is at least one word between the opening and closing tags */
    protected boolean isClosedNonEmpty() {
        return this.isClosingTag() && this.hasLink() && (Math.abs(this.position - this.link.getPosition())) > 0;
    }

//    @Override
    public String toString2() {
        StringBuilder string = new StringBuilder();

        string.append("name:" + name
                + " text:" + text
                + " type:" + type
                + " position:" + position
                + " content:" + content
                + " positions:" + this.getCoveredPositions());

        return string.toString();
    }

}
