package eu.modernmt.decoder.opennmt.memory;

import java.util.Arrays;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScoreEntry entry = (ScoreEntry) o;

        if (domain != entry.domain) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(sentence, entry.sentence)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(translation, entry.translation);
    }

    @Override
    public int hashCode() {
        int result = (int) (domain ^ (domain >>> 32));
        result = 31 * result + Arrays.hashCode(sentence);
        result = 31 * result + Arrays.hashCode(translation);
        return result;
    }

    @Override
    public String toString() {
        return "ScoreEntry{" +
                "domain=" + domain +
                ", sentence=" + Arrays.toString(sentence) +
                ", translation=" + Arrays.toString(translation) +
                '}';
    }
}
