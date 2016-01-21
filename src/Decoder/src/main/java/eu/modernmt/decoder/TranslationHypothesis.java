package eu.modernmt.decoder;

import java.util.List;
import java.util.Map;

/**
 * Created by davide on 30/11/15.
 */
public class TranslationHypothesis extends Sentence implements Comparable<TranslationHypothesis> {

    private float totalScore;
    private Map<String, float[]> scores;

    public TranslationHypothesis(String rawText, float totalScore, Map<String, float[]> scores) {
        super(rawText);
        this.totalScore = totalScore;
        this.scores = scores;
    }

    public TranslationHypothesis(List<String> tokens, float totalScore, Map<String, float[]> scores) {
        super(tokens);
        this.totalScore = totalScore;
        this.scores = scores;
    }

    public TranslationHypothesis(String[] tokens, float totalScore, Map<String, float[]> scores) {
        super(tokens);
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
