package eu.modernmt.decoder.opennmt.memory;

/**
 * Created by davide on 24/05/17.
 */
public class ScoreEntry implements Comparable<ScoreEntry> {

    public final long domain;
    public final String[] sentence;
    public final String[] translation;

    public float score = 0.f;

    public ScoreEntry(long domain, String[] sentence, String[] translation) {
        this.domain = domain;
        this.sentence = sentence;
        this.translation = translation;
    }

    @Override
    public int compareTo(ScoreEntry o) {
        return Float.compare(score, o.score);
    }
}
