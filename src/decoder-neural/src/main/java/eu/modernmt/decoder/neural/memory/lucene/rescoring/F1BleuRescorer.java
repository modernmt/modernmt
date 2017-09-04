package eu.modernmt.decoder.neural.memory.lucene.rescoring;

import eu.modernmt.decoder.neural.memory.lucene.rescoring.util.Ngram;

import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by davide on 06/08/17.
 */
public class F1BleuRescorer implements Rescorer {
    // TODO: Support BLEU other than BLEU=4
    private static final int N = 4;
    private static final double epsilon = 0.1;


    public static double smooth(int num, int den, int count){
        return (num + epsilon) / (den + count * epsilon);
    }

    @Override
    public void rescore(Sentence input, ScoreEntry[] entries) {
        this.rescore(input, entries, null);
    }

    @Override
    public void rescore(Sentence input, ScoreEntry[] entries, ContextVector context) {
        String[] words = TokensOutputStream.toTokensArray(input, false, true);

        computeF1BleuScores(words, entries);

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

    public void computeF1BleuScores(String[] src, ScoreEntry[] entries) {
        int srcLen = src.length;

        // Collect ngram counts of the source sentence into an HashMap where:
        // - key is the ngram
        // - value is the number of occurrences of the ngram in the source sentence
        // It also collects the order of the ngrams
        HashMap srcNgramCounts = new HashMap<>();
        HashMap srcNgramOrders = new HashMap<>();
        for(int order = 1; order <= N; ++order) {
            for(int i = 0; i <= srcLen - order; i++) {
                String[] toks = Arrays.copyOfRange(src, i, i + order);
                Ngram ngram = new Ngram(toks);
                int ngram_id = ngram.hashCode();

                int value = 1;
                Object v = srcNgramCounts.get(ngram_id);
                if (v != null){
                    value = (int) v + 1;
                }
                srcNgramCounts.put(ngram_id, value);
                srcNgramOrders.put(ngram_id, order);
            }
        }

        // Collect ngram counts of each suggestions into an HashMap of HshMaps where:
        // - key is the ngram
        // - value (at the upper level) is a the HashMap, where:
        //   -- key is the idx of the suggestion
        //   -- value is the number of occurrences of the ngram in the specific suggestion
        HashMap suggNgramCounts = new HashMap<>();

        int sugg_id = 0;
        for (ScoreEntry entry : entries) {
            String[] sugg = entry.sentence;
            int suggLen = sugg.length;

            for(int order = 1; order <= N; ++order) {

                for(int i = 0; i <= suggLen - order; ++i) {
                    String[] toks = Arrays.copyOfRange(sugg, i, i + order);
                    Ngram ngram = new Ngram(toks);
                    int ngram_id = ngram.hashCode();

                    HashMap map = (HashMap)  suggNgramCounts.get(ngram_id);
                    int value = 1;
                    if (map == null){
                        map = new HashMap<>();
                        suggNgramCounts.put(ngram_id, map);
                    } else{
                        Object v = map.get(sugg_id);
                        value = (v == null) ? value : (int) v + 1;
                    }
                    map.put(sugg_id, value);
                }
            }
            ++sugg_id;
        }

        // Compute the precision and the recall of the source sentence against each suggestion
        int numer[][] = new int[entries.length][N+1];
        for (HashMap.Entry ngNode : (Set<HashMap.Entry>) srcNgramCounts.entrySet()) {
            int ngram_id = (int) ngNode.getKey();
            int src_count = (int) ngNode.getValue();
            int ngram_order = (int) srcNgramOrders.get(ngram_id);
            HashMap suggCounts = (HashMap) suggNgramCounts.get(ngram_id);
            if (suggCounts != null) {
                for (HashMap.Entry suggNode : (Set<HashMap.Entry>) suggCounts.entrySet()) {
                    sugg_id = (int) suggNode.getKey();
                    int sugg_count = (int) suggNode.getValue();
                    numer[sugg_id][ngram_order] += Math.min(src_count, sugg_count);
                }
            }
        }

        sugg_id = 0;
        for (ScoreEntry entry : entries) {
            int suggLen = entry.sentence.length;

            // compute precision and recall
            double precision = 0;
            double recall = 0;
            for (int order = 1; order <= N; ++order) {
                precision += Math.log(smooth(numer[sugg_id][order], suggLen - order + 1, 1));
                recall += Math.log(smooth(numer[sugg_id][order], srcLen - order + 1, 1));
            }

            precision = Math.exp(precision / N);
            recall = Math.exp(recall / N);

            // compute F1
            entry.score = (float) (2 * ( precision * recall) / (precision + recall));
            ++sugg_id;
        }
    }


//    public static void main(String[] args) {
//
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
//        String sugg_sentence = "This is a suggestion .";
//        String[] sugg_toks = sugg_sentence.split(" ");
//        suggestions[2] = new ScoreEntry(2, sugg_toks, sugg_toks);
//
//
//        sugg_sentence = "One more retrieved suggestion .";
//        sugg_toks = sugg_sentence.split(" ");
//        suggestions[1] = new ScoreEntry(1, sugg_toks, sugg_toks);
//
//
//        sugg_sentence = "This is another suggestion .";
//        sugg_toks = sugg_sentence.split(" ");
//        suggestions[0] = new ScoreEntry(0, sugg_toks, sugg_toks);
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
