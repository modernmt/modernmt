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

}
