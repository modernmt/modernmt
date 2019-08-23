package eu.modernmt.processing.xml.projection;

import eu.modernmt.model.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Set;

class Span implements Comparable<Span>{

    private final int id;
    private final Tag beginTag;
    private final Tag endTag;
    private int level;
    private ArrayList<Integer> positions;
    private int anchor;

    public Span(int id, int level, Tag beginTag, Tag endTag, int words) {
        this.id = id;
        this.beginTag = beginTag;
        this.endTag = endTag;
        this.level = level;

        int begin = Math.min(Math.max(0, getBegin()),words);
        int end = Math.min(words, getEnd());

        this.positions = new ArrayList<>(end - begin);
        for (int i = begin; i < end; i++)
            this.positions.add(i);

        this.anchor = beginTag == null ? getEnd() : getBegin();
//        this.anchor = getBegin();
    }

    public Span(Span span) {
        this.id = span.getId();
        this.beginTag = span.getBeginTag();
        this.endTag = span.getEndTag();
        this.level = span.getLevel();
        this.positions = new ArrayList<>(span.getPositions().size());
        this.positions.addAll(span.getPositions());
        this.anchor = span.getAnchor();
    }

    public int getId() {
        return id;
    }

    public Tag getBeginTag() {
        return beginTag;
    }

    public Tag getEndTag() {
        return endTag;
    }

    public int getAnchor() {
        return anchor;
    }

    public int getBegin() {
        return beginTag == null ? -1 : beginTag.getPosition();
    }

    public int getEnd() {
        return endTag == null ? Integer.MAX_VALUE : endTag.getPosition();
    }

    public String getName() {
        return beginTag == null ? (endTag == null ? null : endTag.getName()) : beginTag.getName();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public ArrayList<Integer> getPositions() {
        return positions;
    }

    public void setAnchor(int anchor) {
        this.anchor = anchor;
    }

    public void addPosition(int position) {
        this.positions.add(position);
    }

    public void addPositions(Set<Integer> positions) {
        this.positions.addAll(positions);
    }

    public void clearPositions() {
        this.positions.clear();
    }

    public void clearPosition(int position) {
        this.positions.remove(position);
    }

    public String toString() {
        String str = "Span id:" + this.id;
        str += " level:" + level;
        str += " begin:" + beginTag;
        str += " end:" + endTag;
        str += " positions:" + positions;
        str += " anchor:" + anchor;
        return str;
    }

//    public boolean isContained(Span span){
//        ArrayList<Integer> spanPositions = span.getPositions();
//        if (this.positions.size() == 0) {
//            if (this.beginTag.getPosition())
//        }
//        if (spanPositions.containsAll(this.getPositions()) ) {
//
//            if (span.beginTag == null) {
//                if (span.endTag == null) {
//                    return true;
//                } else {
//                    if (spanPositions.contains(this.anchor)) {
//                        return true;
//                    } else {
//
//                    }
//                }
//            }
//
//            if (span.endTag == null && this.anchor >= span.getAnchor())
//                return true;
//
//            if (spanPositions.contains(this.anchor))
//                return true;
//        }
//        return false;
//    }

    public boolean isEmpty() {
        return this.positions.size() > 0;
    }



    @Override
    public int compareTo(@NotNull Span a) {
        return this.anchor - a.getAnchor();
    }
}
