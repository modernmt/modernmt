package eu.modernmt.processing.xml;

import eu.modernmt.model.Tag;

import java.util.ArrayList;

/**
 * Created by nicolabertoldi on 18/02/16.
 */
public class MappingTag extends Tag implements Cloneable {

    /* pointer to the corresponding closing (or opening) tag; null if not present or the tag an EMPTY_TAG */
    protected MappingTag link;
    /* list of the word positions covered by the tag */
    protected ArrayList<Integer> coveredPositions;
    /* true if the tag covers at least one position */
    protected boolean content;

    protected MappingTag(String name, String text, boolean leftSpace, String rightSpace, int position, Type type, boolean dtd) {
        super(name, text, leftSpace, rightSpace, position, type, dtd);
        this.link = null;
        this.content = false;
        this.coveredPositions = new ArrayList<>();
    }

    protected MappingTag(String name, String text, boolean leftSpace, String rightSpace, int position, Type type, boolean dtd, MappingTag link, boolean content) {
        super(name, text, leftSpace, rightSpace, position, type, dtd);
        this.link = link;
        this.content = content;
        this.coveredPositions = new ArrayList<>();
    }

    public static MappingTag fromTag(Tag other) {
        return new MappingTag(other.getName(), other.getText(), other.hasLeftSpace(), other.getRightSpace(), other.getPosition(), other.getType(), other.isDTD());
    }

    @Override
    public MappingTag clone() {
        return new MappingTag(name, text, leftSpace, rightSpace, position, type, dtd, link, content);
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

    @Override
    public String toString() {
        return "name:" + name
                + " text:" + text
                + " type:" + type
                + " position:" + position
                + " content:" + content
                + " positions:" + this.getCoveredPositions();
    }

}
