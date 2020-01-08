package eu.modernmt.decoder.neural.memory.lucene.query.rescoring;

import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by davide on 06/08/17.
 */
public class F1BleuRescorer implements Rescorer {

    private static final float MAX_SUGGESTION_EXPANSION = 2.f;

    @Override
    public ScoreEntry[] rescore(LanguageDirection direction, Sentence input, ScoreEntry[] entries, ContextVector context) {
        String[] inputWords = TokensOutputStream.tokens(input, false, true);
        F1BleuCalculator calculator = new F1BleuCalculator(inputWords);

        // Set negative score for suggestions too different in length
        for (ScoreEntry entry : entries) {
            float l1 = inputWords.length;
            float l2 = entry.sentenceTokens.length;

            float expansion = Math.max(l1, l2) / Math.min(l1, l2);

            if (expansion > MAX_SUGGESTION_EXPANSION)
                entry.score = -1;
        }

        // Compute F1-BLEU score
        for (ScoreEntry entry : entries) {
            if (entry.score >= 0)
                entry.score = calculator.calc(entry.sentenceTokens);
        }

        // Apply context scores
        HashMap<Long, Float> contextScores = new HashMap<>();

        if (context != null && context.size() > 0) {
            for (ContextVector.Entry ce : context)
                contextScores.put(ce.memory.getId(), ce.score);
        }

        for (ScoreEntry entry : entries) {
            if (entry.score >= 0) {
                Float contextScore = contextScores.get(entry.memory);
                entry.score = entry.score * .5f + (contextScore == null ? 0.f : contextScore) * .5f;
            }
        }

        for (ScoreEntry entry : entries) {
            if (entry.score < 0)
                entry.score = 0;
        }

        Arrays.sort(entries);
        ArrayUtils.reverse(entries);

        return entries;
    }

}
