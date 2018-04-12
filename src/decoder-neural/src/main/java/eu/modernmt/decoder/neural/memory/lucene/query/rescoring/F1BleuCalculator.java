package eu.modernmt.decoder.neural.memory.lucene.query.rescoring;

import java.util.HashMap;
import java.util.Map;

public class F1BleuCalculator {

    private static final int N = 4;
    private static final double EPSILON = 0.1;
    private final String[] reference;
    private final HashMap<NGram, Counter> referenceNGrams;

    public F1BleuCalculator(String[] reference) {
        this.reference = reference;
        this.referenceNGrams = split(this.reference, N);
    }

    public float calc(String[] hyp) {
        HashMap<NGram, Counter> hypNGrams = split(hyp, N);
        return getF1BleuScore(this.referenceNGrams, this.reference.length, hypNGrams, hyp.length);
    }

    private static HashMap<NGram, Counter> split(String[] sentence, int order) {
        HashMap<NGram, Counter> counts = new HashMap<>(sentence.length * order);

        for (int offset = 0; offset < sentence.length; offset++) {
            int maxOrder = sentence.length - offset;

            for (int o = 1; o <= Math.min(order, maxOrder); o++) {
                NGram ngram = new NGram(sentence, offset, o);
                counts.computeIfAbsent(ngram, key -> new Counter()).value++;
            }
        }

        return counts;
    }

    private static float getF1BleuScore(HashMap<NGram, Counter> sentence, int sentenceLength, HashMap<NGram, Counter> suggestion, int suggestionLength) {
        int numerators[] = new int[N];

        for (Map.Entry<NGram, Counter> entry : sentence.entrySet()) {
            NGram ngram = entry.getKey();

            int order = ngram.getOrder();
            int count = entry.getValue().value;
            int suggestionCount = suggestion.getOrDefault(ngram, Counter.ZERO).value;

            numerators[order - 1] += Math.min(count, suggestionCount);
        }

        double precision = 0;
        double recall = 0;

        for (int order = 1; order <= N; ++order) {
            precision += Math.log(smooth(numerators[order - 1], Math.max(suggestionLength - order + 1, 0), 1));
            recall += Math.log(smooth(numerators[order - 1], Math.max(sentenceLength - order + 1, 0), 1));
        }

        precision = Math.exp(precision / N);
        recall = Math.exp(recall / N);

        // compute F1
        return (float) (2 * (precision * recall) / (precision + recall));
    }

    private static double smooth(int num, int den, int count) {
        return (num + EPSILON) / (den + count * EPSILON);
    }

    private static final class Counter {

        public static final Counter ZERO = new Counter();

        public int value = 0;
    }

    private static final class NGram {

        private final String[] sentence;
        private final int offset;
        private final int order;

        private int hash = 0;

        public NGram(String[] sentence, int offset, int order) {
            this.sentence = sentence;
            this.offset = offset;
            this.order = order;
        }

        public int getOrder() {
            return order;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NGram ngram = (NGram) o;

            if (order != ngram.order) return false;

            for (int i = 0; i < order; i++) {
                String a = sentence[i + offset];
                String b = ngram.sentence[i + ngram.offset];

                if (!(a != null && b != null && a.equals(b)))
                    return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                int result = 1;

                for (int i = 0; i < order; i++) {
                    String element = sentence[i + offset];
                    result = 31 * result + (element == null ? 0 : element.hashCode());
                }

                hash = result;
            }

            return hash;
        }

        @Override
        public String toString() {
            StringBuilder string = new StringBuilder("(");
            for (int i = 0; i < order; i++) {
                if (i > 0)
                    string.append(' ');
                string.append(sentence[i + offset]);
            }
            string.append(')');

            return string.toString();
        }

    }

}
