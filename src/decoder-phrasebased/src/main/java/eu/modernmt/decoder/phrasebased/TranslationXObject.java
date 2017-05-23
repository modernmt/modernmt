package eu.modernmt.decoder.phrasebased;

import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by davide on 02/12/15.
 */
class TranslationXObject {

    static class Hypothesis {
        public String text;
        public float totalScore;
        public String fvals;

        public Hypothesis(String text, float totalScore, String fvals) {
            this.text = text;
            this.totalScore = totalScore;
            this.fvals = fvals;
        }

        public TranslationHypothesis getTranslationHypothesis(Sentence source, HashMap<String, DecoderFeature> features) {
            HashMap<DecoderFeature, float[]> scores = new HashMap<>();

            String[] tokens = fvals.trim().split("\\s+");

            DecoderFeature feature = null;
            float[] weights = new float[tokens.length];
            int index = 0;

            for (int i = 0; i < tokens.length + 1; i++) {
                String token = i < tokens.length ? tokens[i] : null;

                if (token == null || token.endsWith("=")) {
                    if (feature != null)
                        scores.put(feature, Arrays.copyOf(weights, index));

                    if (token != null)
                        feature = features.get(token.substring(0, token.length() - 1));

                    index = 0;
                } else {
                    weights[index++] = Float.parseFloat(token);
                }
            }

            return new TranslationHypothesis(XUtils.explode(this.text), source, null, this.totalScore, scores);
        }
    }

    public String text;
    public Hypothesis[] nbestList;
    public int[] alignment;

    public TranslationXObject(String text, Hypothesis[] nbestList, int[] alignment) {
        this.text = text;
        this.nbestList = nbestList;
        this.alignment = alignment;
    }

    public Translation getTranslation(Sentence source, HashMap<String, DecoderFeature> features) {
        Word[] words = XUtils.explode(text);

        Translation translation = new Translation(words, source, XUtils.decodeAlignment(alignment));

        if (nbestList != null && nbestList.length > 0) {
            List<Translation> nbest = new ArrayList<>(nbestList.length);

            for (Hypothesis hyp : nbestList)
                nbest.add(hyp.getTranslationHypothesis(source, features));

            translation.setNbest(nbest);
        }

        return translation;
    }

}
