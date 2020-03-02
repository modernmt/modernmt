package eu.modernmt.memory;

import eu.modernmt.lang.LanguageDirection;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by davide on 24/05/17.
 */
public class ScoreEntry implements Comparable<ScoreEntry> {

    public final long memory;
    public final LanguageDirection language;
    public final String[] sentenceTokens;
    public final String[] translationTokens;

    public float auxiliaryScore = 0.f;
    public float score = 0.f;

    public ScoreEntry(long memory, LanguageDirection language, String[] sentenceTokens, String[] translationTokens) {
        this.memory = memory;
        this.language = language;
        this.sentenceTokens = sentenceTokens;
        this.translationTokens = translationTokens;
    }

    @Override
    public int compareTo(ScoreEntry o) {
        return Float.compare(score, o.score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoreEntry that = (ScoreEntry) o;
        return memory == that.memory &&
                Objects.equals(language, that.language) &&
                Arrays.equals(sentenceTokens, that.sentenceTokens) &&
                Arrays.equals(translationTokens, that.translationTokens);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(memory, language);
        result = 31 * result + Arrays.hashCode(sentenceTokens);
        result = 31 * result + Arrays.hashCode(translationTokens);
        return result;
    }

    @Override
    public String toString() {
        return "ScoreEntry{" +
                "memory=" + memory +
                ", language=" + language +
                ", sentence=" + Arrays.toString(sentenceTokens) +
                ", translation=" + Arrays.toString(translationTokens) +
                ", score=" + score +
                '}';
    }

}
