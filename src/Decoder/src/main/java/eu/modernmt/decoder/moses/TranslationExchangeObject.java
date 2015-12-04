package eu.modernmt.decoder.moses;

import eu.modernmt.decoder.Sentence;
import eu.modernmt.decoder.TranslationHypothesis;

import java.util.*;

/**
 * Created by davide on 02/12/15.
 */
class TranslationExchangeObject {

    private static List<TranslationHypothesis.Score> parse(String fvals) {
        List<TranslationHypothesis.Score> result = new ArrayList<>();

        String[] tokens = fvals.split("\\s+");

        String feature = null;
        float[] scores = new float[tokens.length];
        int index = 0;

        for (int i = 0; i < tokens.length + 1; i++) {
            String token = i < tokens.length ? tokens[i] : null;

            if (token == null || token.endsWith("=")) {
                if (feature != null) {
                    TranslationHypothesis.Score score = new TranslationHypothesis.Score(feature, Arrays.copyOf(scores, index));
                    result.add(score);
                }

                if (token != null)
                    feature = token.substring(0, token.length() - 1);

                index = 0;
            } else {
                scores[index++] = Float.parseFloat(token);
            }
        }

        return result;
    }

    public List<TranslationHypothesis> getHypotheses(Sentence source) {
        List<TranslationHypothesis> result = new ArrayList<>(nbestList.size());

        for (Hypothesis hyp : nbestList) {
            List<TranslationHypothesis.Score> scores = parse(hyp.fvals);
            result.add(new TranslationHypothesis(hyp.text, source, hyp.totalScore, scores));
        }

        return result;
    }

    static class Hypothesis {
        public String text;
        public float totalScore;
        public String fvals;
    }

    public String text;
    public List<Hypothesis> nbestList;

}
