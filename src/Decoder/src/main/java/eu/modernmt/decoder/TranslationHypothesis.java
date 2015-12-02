package eu.modernmt.decoder;

import java.util.List;

/**
 * Created by davide on 30/11/15.
 */
public class TranslationHypothesis {

    private String text;
    private float totalScore;
    private List<DecoderFeature> scores;

    public TranslationHypothesis(String text, float totalScore, List<DecoderFeature> scores) {
        this.text = text;
        this.totalScore = totalScore;
        this.scores = scores;
    }

    public String getText() {
        return text;
    }

    public float getTotalScore() {
        return totalScore;
    }

    public List<DecoderFeature> getScores() {
        return scores;
    }
}
