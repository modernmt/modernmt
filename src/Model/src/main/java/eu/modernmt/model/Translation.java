package eu.modernmt.model;

/**
 * Created by davide on 17/02/16.
 */
public class Translation extends Sentence {

    protected final Sentence source;
    private final int[][] alignment;

    public Translation(Token[] tokens, Sentence source, int[][] alignment) {
        super(tokens);
        this.source = source;
        this.alignment = alignment;
    }

    public Translation(Token[] tokens, Tag[] tags, Sentence source, int[][] alignment) {
        super(tokens, tags);
        this.source = source;
        this.alignment = alignment;
    }

    public Sentence getSource() {
        return source;
    }

    public int[][] getAlignment() {
        return alignment;
    }

}
