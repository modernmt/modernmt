package eu.modernmt.processing.tags;

/**
 * Created by davide on 17/02/16.
 */
public class _Translation extends _Sentence {

    protected final _Sentence source;
    private final int[][] alignment;

    public _Translation(_Token[] tokens, _Sentence source, int[][] alignment) {
        super(tokens);
        this.source = source;
        this.alignment = alignment;
    }

    public _Translation(_Token[] tokens, _Tag[] tags, _Sentence source, int[][] alignment) {
        super(tokens, tags);
        this.source = source;
        this.alignment = alignment;
    }

    public _Sentence getSource() {
        return source;
    }

    public int[][] getAlignment() {
        return alignment;
    }

}
