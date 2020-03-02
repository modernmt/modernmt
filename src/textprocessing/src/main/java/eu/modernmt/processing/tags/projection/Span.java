package eu.modernmt.processing.tags.projection;

import eu.modernmt.model.Tag;

class Span implements Comparable<Span> {

    private final int id;
    private final Tag beginTag;
    private final Tag endTag;
    private int level;
    private Coverage positions;
    private int anchor;

    Span(int id, int level, Tag beginTag, Tag endTag, int words) {
        this.id = id;
        this.beginTag = beginTag;
        this.endTag = endTag;
        this.level = level;

        int begin = Math.min(Math.max(0, getBegin()), words);
        int end = Math.min(words, getEnd());

        this.positions = new Coverage(end - begin);
        for (int i = begin; i < end; i++)
            this.positions.add(i);

        this.anchor = beginTag == null ? getEnd() : getBegin();
    }

    Span(Span span) {
        this.id = span.getId();
        this.beginTag = span.getBeginTag();
        this.endTag = span.getEndTag();
        this.level = span.getLevel();
        this.positions = new Coverage(span.getPositions());
        this.positions.addAll(span.getPositions());
        this.anchor = span.getAnchor();
    }

    public int getId() {
        return id;
    }

    Tag getBeginTag() {
        return beginTag;
    }

    Tag getEndTag() {
        return endTag;
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

    int getAnchor() {
        return anchor;
    }

    void setAnchor(int anchor) {
        this.anchor = anchor;
    }

    public Coverage getPositions() {
        return positions;
    }

    void addPositions(Coverage positions) {
        this.positions.addAll(positions);
    }


    void clearPositions() {
        this.positions.clear();
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

    public boolean isEmpty() {
        return this.positions.isEmpty();
    }

    @Override
    public int compareTo(Span a) {
        return this.anchor - a.getAnchor();
    }
}