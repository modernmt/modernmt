package eu.modernmt.processing.tags;

import java.util.ArrayList;

/**
 * Created by nicolabertoldi on 18/02/16.
 */
public class _mappingTag extends _Tag{

    protected int link; //pointer to the closing (or opening tag) for not SELF_CONTAINED tags; -1 if undefined
    protected int parent;
    protected ArrayList<Integer> coveredPositions; //list of the word positions covered by the tag


    public _mappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, String name) {
        super(text, rightSpace, leftSpace, position, type, name);
        this.link = -1;
        this.parent = -1;
        this.coveredPositions = new ArrayList<>();
    }

    public _mappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, String name, int link) {
        super(text, rightSpace, leftSpace, position, type, name);
        this.link = link;
        this.parent = -1;
        this.coveredPositions = new ArrayList<>();
    }

    public _mappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, String name, int link, int parent) {
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

    public int getLink() { return link; }
    public int getParent() { return parent; }
    public ArrayList<Integer> getCoveredPositions() { return coveredPositions; }

    public void setLink(int link) {
        this.link = link;
    }
    public void setParent(int parent) {
        this.parent = parent;
    }
    public void setCoveredPositions(ArrayList<Integer> coveredPositions) {
        this.coveredPositions.addAll(coveredPositions);
    }
    public int compareTo(_mappingTag compareTag) {
        return Integer.compare(this.parent, compareTag.getParent());
    }
}
