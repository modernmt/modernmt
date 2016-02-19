package eu.modernmt.processing.tags;

import java.util.ArrayList;

/**
 * Created by nicolabertoldi on 18/02/16.
 */
public class _mappingTag extends _Tag{

    protected enum Type {
        CONTAINS_NONEMPTY_TEXT,     // ex: <a>data</a>
        CONTAINS_EMPTY_TEXT,        // ex: <a></a>
        OPENED_NONEMPTY_TEXT,     // ex: <a>data</a>
        CLOSED_NONEMPTY_TEXT,     // ex: <a>data</a>
        OPENED_EMPTY_TEXT,        // ex: <a></a>
        CLOSED_EMPTY_TEXT,        // ex: <a></a>
        SELF_CONTAINED,        // ex: <a/>
        // two additional types to handle broken markup:
        OPENED_BUT_UNCLOSED,        // ex: <a>
        CLOSED_BUT_UNOPENED,        // ex: </a>
        UNDEF      // still undefined
    };

    //note that position of
    protected Type type;
    protected String name; //name of the tag (without triangle brackets, backslash, and attributes
    
    protected int link; //pointer to the closing (or opening tag) for not SELF_CONTAINED tags; -1 if undefined
    protected int parent;
    protected ArrayList<Integer> coveredPositions; //list of the word positions covered by the tag


    public _mappingTag(String text, boolean leftSpace, boolean rightSpace, int position) {
        super(text, rightSpace, leftSpace, position);
        this.type = Type.UNDEF;
        this.link = -1;
        this.parent = -1;
        this.name = "";
        this.coveredPositions = new ArrayList<>();
    }

    public _mappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type) {
        super(text, rightSpace, leftSpace, position);
        this.type = type;
        this.parent = -1;
        this.name = "";
        this.coveredPositions = new ArrayList<>();
    }

    public _mappingTag(String text, boolean leftSpace, boolean rightSpace, int position, Type type, int link) {
        super(text, rightSpace, leftSpace, position);
        this.type = type;
        this.link = link;
        this.parent = -1;
        this.name = "";
        this.coveredPositions = new ArrayList<>();
    }

    public static _mappingTag fromTag(_Tag fromTag) {
        return new _mappingTag(fromTag.getText(), fromTag.hasLeftSpace(), fromTag.hasRightSpace(), fromTag.getPosition());
    }

    public Type getType() { return type; }
    public int getLink() { return link; }
    public int getParent() { return parent; }
    public String getName() { return name; }
    public ArrayList<Integer> getCoveredPositions() { return coveredPositions; }

    public void setType(Type type) {
        this.type = type;
    }
    public void setLink(int link) {
        this.link = link;
    }
    public void setParent(int parent) {
        this.parent = parent;
    }
    public void setName(String name) { this.name = name; }
    public void setCoveredPositions(ArrayList<Integer> coveredPositions) {
        this.coveredPositions.addAll(coveredPositions);
    }
    public int compareTo(_mappingTag compareTag) {
        return Integer.compare(this.parent, compareTag.getParent());
    }
}
