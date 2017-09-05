package eu.modernmt.decoder.neural.memory.lucene.rescoring;

import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

/**
 * Created by davide on 06/08/17.
 */
public class F1BleuRescorer implements Rescorer {

    private static final int N = 4;
    private static final double EPSILON = 0.1;

    @Override
    public void rescore(Sentence input, ScoreEntry[] entries) {
        this.rescore(input, entries, null);
    }

    @Override
    public void rescore(Sentence input, ScoreEntry[] entries, ContextVector context) {
        String[] inputWords = TokensOutputStream.toTokensArray(input, false, true);

        // Compute F1-BLEU score
        HashMap<NGram, Counter> inputNGrams = split(inputWords);

        for (ScoreEntry entry : entries) {
            HashMap<NGram, Counter> entryNGrams = split(entry.sentence);
            entry.score = getF1BleuScore(inputNGrams, inputWords.length, entryNGrams, entry.sentence.length);
        }

        // Apply context scores if possible
        if (context != null && context.size() > 0) {
            HashMap<Long, Float> contextScores = new HashMap<>(context.size());
            for (ContextVector.Entry ce : context) {
                contextScores.put(ce.domain.getId(), ce.score);
            }

            for (ScoreEntry entry : entries) {
                Float contextScore = contextScores.get(entry.domain);
                entry.score = entry.score * .5f + (contextScore == null ? 0.f : contextScore) * .5f;
            }
        }

        Arrays.sort(entries);
        ArrayUtils.reverse(entries);
    }

    private static HashMap<NGram, Counter> split(String[] sentence) {
        List<NGram> ngrams = NGram.split(sentence, N);
        HashMap<NGram, Counter> counts = new HashMap<>(ngrams.size());

        for (NGram ngram : ngrams) {
            counts.computeIfAbsent(ngram, key -> new Counter()).value++;
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

        public static List<NGram> split(String[] sentence, int order) {
            ArrayList<NGram> ngrams = new ArrayList<>(sentence.length * order);

            for (int offset = 0; offset < sentence.length; offset++) {
                int maxOrder = sentence.length - offset;

                for (int o = 1; o <= Math.min(order, maxOrder); o++) {
                    ngrams.add(new NGram(sentence, offset, o));
                }
            }

            return ngrams;
        }

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

//    public static void main(String[] args) {
//        String src_sentence = "This is a hypothesis sentence .";
//
//        String[] src_toks = src_sentence.split(" ");
//        Word[] src_words = new Word[src_toks.length];
//
//        for (int i = 0; i < src_toks.length; ++i)
//            src_words[i] = new Word(src_toks[i]);
//
//        Sentence input = new Sentence(src_words);
//
//        ScoreEntry[] suggestions = new ScoreEntry[4];
//        String sugg_sentence = "This is another suggestion .";
//        String[] sugg_toks = sugg_sentence.split(" ");
//        suggestions[0] = new ScoreEntry(0, sugg_toks, sugg_toks);
//
//        sugg_sentence = "Short suggestion";
//        sugg_toks = sugg_sentence.split(" ");
//        suggestions[1] = new ScoreEntry(1, sugg_toks, sugg_toks);
//
//
//        sugg_sentence = "This is another suggestion .";
//        sugg_toks = sugg_sentence.split(" ");
//        suggestions[2] = new ScoreEntry(2, sugg_toks, sugg_toks);
//
//        sugg_sentence = "This is a hypothesis sentence .";
//        sugg_toks = sugg_sentence.split(" ");
//        suggestions[3] = new ScoreEntry(3, sugg_toks, sugg_toks);
//
//        System.err.println("src:" + src_sentence);
//
//        System.err.println("suggestions before rescoring");
//        for (ScoreEntry sugg : suggestions) {
//            System.err.println("sugg: " + sugg);
//        }
//
//        F1BleuRescorer scorer = new F1BleuRescorer();
//        scorer.rescore(input, suggestions);
//
//        System.err.println("suggestions after rescoring");
//        for (ScoreEntry sugg : suggestions) {
//            System.err.println("sugg: " + sugg);
//        }
//    }

}
