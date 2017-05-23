package eu.modernmt.decoder.phrasebased;

import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.decoder.HasFeatureScores;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public class TranslationHypothesis extends Translation implements HasFeatureScores {

    private float totalScore;
    private Map<DecoderFeature, float[]> scores;

    public TranslationHypothesis(Word[] words, Sentence source, Alignment alignment, float totalScore, Map<DecoderFeature, float[]> scores) {
        super(words, source, alignment);
        this.totalScore = totalScore;
        this.scores = scores;
    }

    public float getTotalScore() {
        return totalScore;
    }

    public Map<DecoderFeature, float[]> getScores() {
        return scores;
    }

    @Override
    public int compareTo(HasFeatureScores o) {
        return Float.compare(totalScore, o.getTotalScore());
    }
}