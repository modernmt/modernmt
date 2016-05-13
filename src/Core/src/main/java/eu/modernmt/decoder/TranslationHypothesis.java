package eu.modernmt.decoder;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

import java.util.Map;

/**
 * Created by davide on 30/11/15.
 */
public class TranslationHypothesis extends Translation implements Comparable<TranslationHypothesis> {

    private float totalScore;
    private Map<String, float[]> scores;

    public TranslationHypothesis(Word[] words, Sentence source, int[][] alignment, float totalScore, Map<String, float[]> scores) {
        super(words, source, alignment);
        this.totalScore = totalScore;
        this.scores = scores;
    }

    public TranslationHypothesis(Word[] words, Tag[] tags, Sentence source, int[][] alignment, float totalScore, Map<String, float[]> scores) {
        super(words, tags, source, alignment);
        this.totalScore = totalScore;
        this.scores = scores;
    }

    @Override
    public int compareTo(TranslationHypothesis o) {
        return Float.compare(totalScore, o.totalScore);
    }

    public float getTotalScore() {
        return totalScore;
    }

    public Map<String, float[]> getScores() {
        return scores;
    }

    public String getTranslation() {
        return super.toString();
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        string.append(super.toString());
        string.append('{');

        for (Map.Entry<String, float[]> score : scores.entrySet()) {
            string.append(score.getKey());
            string.append(':');

            float[] weights = score.getValue();
            for (int i = 0; i < weights.length; i++) {
                string.append(weights[i]);
                if (i < weights.length - 1)
                    string.append(' ');
            }

            string.append(", ");
        }

        return string.substring(0, string.length()) + '}';
    }
}
