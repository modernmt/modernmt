package eu.modernmt.decoder.neural.memory;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * Created by davide on 24/05/17.
 */
public class ScoreEntry implements Comparable<ScoreEntry> {

    public final long memory;
    public final String[] sentence;
    public final String[] translation;

    public float score = 0.f;

    public ScoreEntry(long memory, String[] sentence, String[] translation) {
        this.memory = memory;
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

        if (memory != entry.memory) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(sentence, entry.sentence)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(translation, entry.translation);
    }

    @Override
    public int hashCode() {
        int result = (int) (memory ^ (memory >>> 32));
        result = 31 * result + Arrays.hashCode(sentence);
        result = 31 * result + Arrays.hashCode(translation);
        return result;
    }

    @Override
    public String toString() {
        return "ScoreEntry{" +
                "memory=" + memory +
                ", sentence=" + StringUtils.join(sentence, ' ') +
                ", translation=" + StringUtils.join(translation, ' ') +
                ", score=" + score +
                '}';
    }
}
