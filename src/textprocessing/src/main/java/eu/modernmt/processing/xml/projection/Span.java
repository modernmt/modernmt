package eu.modernmt.processing.xml.projection;

import eu.modernmt.model.Tag;

import java.util.ArrayList;

class Span {

    private final int id;
    private final Tag beginTag;
    private final Tag endTag;
    private int level;
    private final ArrayList<Integer> positions;

    public Span(int id, int level, Tag beginTag, Tag endTag, int words) {
        this.id = id;
        this.beginTag = beginTag;
        this.endTag = endTag;
        this.level = level;

        int begin = Math.max(0, getBegin());
        int end = Math.min(words, getEnd());

        this.positions = new ArrayList<>(end - begin);
        for (int i = begin; i < end; i++)
            this.positions.add(i);
    }

    public int getId() {
        return id;
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

}
