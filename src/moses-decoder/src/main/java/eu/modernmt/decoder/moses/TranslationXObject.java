package eu.modernmt.decoder.moses;

import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by davide on 02/12/15.
 */
class TranslationXObject {

    private static Word[] explode(String text) {
        String[] pieces = text.split(" +");
        Word[] words = new Word[pieces.length];

        for (int i = 0; i < pieces.length; i++) {
            String rightSpace = i < pieces.length - 1 ? " " : null;

            long id = Long.parseLong(pieces[i]);
            words[i] = new Word((int) (id), rightSpace);
        }

        return words;
    }

    static class Hypothesis {
        public String text;
        public float totalScore;
        public String fvals;

        public Hypothesis(String text, float totalScore, String fvals) {
            this.text = text;
            this.totalScore = totalScore;
            this.fvals = fvals;
        }

        public TranslationHypothesis getTranslationHypothesis(Sentence source) {
            HashMap<String, float[]> scores = new HashMap<>();

            String[] tokens = fvals.trim().split("\\s+");

            String feature = null;
            float[] weights = new float[tokens.length];
            int index = 0;

            for (int i = 0; i < tokens.length + 1; i++) {
                String token = i < tokens.length ? tokens[i] : null;

                if (token == null || token.endsWith("=")) {
                    if (feature != null)
                        scores.put(feature, Arrays.copyOf(weights, index));

                    if (token != null)
                        feature = token.substring(0, token.length() - 1);

                    index = 0;
                } else {
                    weights[index++] = Float.parseFloat(token);
                }
            }

            return new TranslationHypothesis(explode(this.text), source, null, this.totalScore, scores);
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

    public DecoderTranslation getTranslation(Sentence source) {
        Word[] words = explode(text);

        DecoderTranslation translation = new DecoderTranslation(words, source, parseAlignment(alignment));

        if (nbestList != null && nbestList.length > 0) {
            List<TranslationHypothesis> nbest = new ArrayList<>(nbestList.length);

            for (Hypothesis hyp : nbestList)
                nbest.add(hyp.getTranslationHypothesis(source));

            translation.setNbest(nbest);
        }

        return translation;
    }

    private static Alignment parseAlignment(int[] encoded) {
        if (encoded == null || encoded.length == 0)
            return null;

        int size = encoded.length / 2;

        int[] source = new int[size];
        int[] target = new int[size];

        System.arraycopy(encoded, 0, source, 0, size);
        System.arraycopy(encoded, size, target, 0, size);

        return new Alignment(source, target);
    }

}
