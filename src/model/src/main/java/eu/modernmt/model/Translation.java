package eu.modernmt.model;

/**
 * Created by davide on 17/02/16.
 */
public class Translation extends Sentence {

    protected final Sentence source;
    private Alignment alignment;
    private long elapsedTime;

    public Translation(Word[] words, Sentence source, Alignment alignment) {
        this(words, null, source, alignment);
    }

    public Translation(Word[] words, Tag[] tags, Sentence source, Alignment alignment) {
        super(words, tags);
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

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public boolean hasAlignment() {
        return alignment != null && alignment.size() > 0;
    }

}
