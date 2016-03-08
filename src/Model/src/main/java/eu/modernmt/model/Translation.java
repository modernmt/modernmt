package eu.modernmt.model;

/**
 * Created by davide on 17/02/16.
 */
public class Translation extends Sentence {

    protected final Sentence source;
    private final int[][] alignment;
    private long elapsedTime;

    public Translation(Token[] tokens, Sentence source, int[][] alignment) {
        this(tokens, null, source, alignment);
    }

    public Translation(Token[] tokens, Tag[] tags, Sentence source, int[][] alignment) {
        super(tokens, tags);
        this.source = source;
        this.alignment = alignment;
        this.elapsedTime = 0;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public Sentence getSource() {
        return source;
    }

    public int[][] getAlignment() {
        return alignment;
    }

    public boolean hasAlignment() {
        return alignment != null && alignment.length > 0;
    }

}
