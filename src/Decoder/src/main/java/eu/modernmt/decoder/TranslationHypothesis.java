package eu.modernmt.decoder;

import java.util.List;

/**
 * Created by davide on 30/11/15.
 */
public class TranslationHypothesis extends Translation implements Comparable<TranslationHypothesis> {

    private float totalScore;
    private List<Score> scores;

    public TranslationHypothesis(String rawText, Sentence source, float totalScore, List<Score> scores) {
        super(rawText, source);
        this.totalScore = totalScore;
        this.scores = scores;
    }

    public TranslationHypothesis(List<String> tokens, Sentence source, float totalScore, List<Score> scores) {
        super(tokens, source);
        this.totalScore = totalScore;
        this.scores = scores;
    }

    public TranslationHypothesis(String[] tokens, Sentence source, float totalScore, List<Score> scores) {
        super(tokens, source);
        this.totalScore = totalScore;
        this.scores = scores;
    }

    @Override
    public int compareTo(TranslationHypothesis o) {
        return Float.compare(totalScore, o.totalScore);
    }

    public static class Score {
        public final String component;
        public final float[] scores;

        public Score(String component, float[] scores) {
            this.component = component;
            this.scores = scores;
        }
    }

    public float getTotalScore() {
        return totalScore;
    }

    public List<Score> getScores() {
        return scores;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        string.append(super.toString());
        string.append('{');

        for (int j = 0; j < scores.size(); j++) {
            Score score = scores.get(j);
            string.append(score.component);
            string.append(':');

            for (int i = 0; i < score.scores.length; i++) {
                string.append(score.scores[i]);
                if (i < score.scores.length - 1)
                    string.append(' ');
            }

            if (j < scores.size() - 1)
                string.append(", ");
        }

        string.append('}');
        return string.toString();
    }
}
