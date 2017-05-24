package eu.modernmt.decoder.opennmt.storage;

/**
 * Created by davide on 24/05/17.
 */
public class ScoreEntry implements Comparable<ScoreEntry> {

    public final int domain;
    public final String[] sentence;
    public final String[] translation;

    public float score = 0.f;

    public ScoreEntry(int domain, String[] sentence, String[] translation) {
        this.domain = domain;
        this.sentence = sentence;
        this.translation = translation;
    }

    @Override
    public int compareTo(ScoreEntry o) {
        return Float.compare(score, o.score);
    }
}
