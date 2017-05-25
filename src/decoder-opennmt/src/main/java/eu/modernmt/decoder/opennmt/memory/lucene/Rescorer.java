package eu.modernmt.decoder.opennmt.memory.lucene;

import eu.modernmt.decoder.opennmt.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by davide on 24/05/17.
 */
class Rescorer {

    public void score(Sentence input, ScoreEntry[] entries) {
        score(input, entries, null);
    }

    public void score(Sentence input, ScoreEntry[] entries, ContextVector context) {
        String[] words = TokensOutputStream.toTokensArray(input, false, true);

        for (ScoreEntry entry : entries)
            entry.score = 1.f - getLevenshteinDistance(words, entry.sentence);

        if (context != null && context.size() > 0) {
            HashMap<Integer, Float> contextScores = new HashMap<>(context.size());
            for (ContextVector.Entry ce : context)
                contextScores.put(ce.domain.getId(), ce.score);

            for (ScoreEntry entry : entries) {
                Float contextScore = contextScores.get(entry.domain);
                entry.score = entry.score * .5f + (contextScore == null ? 0.f : contextScore) * .5f;
            }
        }

        Arrays.sort(entries);
        ArrayUtils.reverse(entries);
    }

    private static float getLevenshteinDistance(String[] s, String[] t) {
        // degenerate cases
        if (Arrays.equals(s, t)) return 0;
        if (s.length == 0) return t.length;
        if (t.length == 0) return s.length;

        // create two work vectors of integer distances
        int[] v0 = new int[t.length + 1];
        int[] v1 = new int[t.length + 1];

        // initialize v0 (the previous row of distances)
        // this row is A[0][i]: edit distance for an empty s
        // the distance is just the number of characters to delete from t
        for (int i = 0; i < v0.length; i++)
            v0[i] = i;

        for (int i = 0; i < s.length; i++) {
            // calculate v1 (current row distances) from the previous row v0

            // first element of v1 is A[i+1][0]
            //   edit distance is delete (i+1) chars from s to match empty t
            v1[0] = i + 1;

            // use formula to fill in the rest of the row
            for (int j = 0; j < t.length; j++) {
                int cost = (s[i].equals(t[j])) ? 0 : 1;
                v1[j + 1] = min(v1[j] + 1, v0[j + 1] + 1, v0[j] + cost);
            }

            // copy v1 (current row) to v0 (previous row) for next iteration
            System.arraycopy(v1, 0, v0, 0, v0.length);
        }

        float distance = v1[t.length];
        return distance / Math.max(s.length, t.length);
    }

    private static int min(int a, int b, int c) {
        int z = (a <= b) ? a : b;
        return (z <= c) ? z : c;
    }

}
