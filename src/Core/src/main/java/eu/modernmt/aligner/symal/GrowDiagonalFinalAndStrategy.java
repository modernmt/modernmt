package eu.modernmt.aligner.symal;

import java.util.Arrays;
import java.util.Set;

/**
 * Created by davide on 20/05/16.
 */
public class GrowDiagonalFinalAndStrategy implements SymmetrizationStrategy {


    @Override
    public int[][] symmetrize(int[][] forward, int[][] backward) {
        Arrays.sort(forward, AlignmentMatrix.Sorter.instance);
        Arrays.sort(backward, AlignmentMatrix.Sorter.instance);

        AlignmentMatrix intersect = AlignmentMatrix.build(forward, backward)
                .or(forward)
                .and(backward);
        AlignmentMatrix union = AlignmentMatrix.build(forward, backward)
                .or(forward)
                .or(backward);

        intersect = GrowDiagonalStrategy.symmetrize(intersect, union);

        // Forward final and
        for (int f = 0; f < intersect.getForwardSize(); f++) {
            for (int[] el : forward) {
                if (el[0] != f)
                    continue;

                if (!intersect.get(f, el[1]) && !intersect.isSourceWordAligned(f) && !intersect.isTargetWordAligned(el[1])) {
                    intersect.set(f, el[1]);
                    break;
                }
            }
        }

        // Backward final and
        for (int b = 0; b < intersect.getBackwardSize(); b++) {
            for (int[] el : backward) {
                if (el[1] != b)
                    continue;

                if (!intersect.get(el[0], b) && !intersect.isSourceWordAligned(el[0]) && !intersect.isTargetWordAligned(b)) {
                    intersect.set(el[0], b);
                    break;
                }
            }
        }

        return intersect.toArray();
    }

    static public void FinalAndForward(boolean[][] currentpoints, Set<Integer> unaligned_s, Set<Integer> unaligned_t, Set<Integer>[] s2t) {
        for (int s_word = 0; s_word < s2t.length; s_word++) {
            if (s2t[s_word] != null) {
                for (Integer t_word : s2t[s_word]) {
                    if (!currentpoints[s_word][t_word]) {
                        if (unaligned_s.contains(s_word) && unaligned_t.contains(t_word)) {
                            currentpoints[s_word][t_word] = true;
                            unaligned_s.remove(s_word);
                            unaligned_t.remove(t_word);
                            break;
                        }
                    }
                }
            }
        }
    }

    static public void FinalAndBackward(boolean[][] currentpoints, Set<Integer> unaligned_s, Set<Integer> unaligned_t, Set<Integer>[] t2s) {
        for (int t_word = 0; t_word < t2s.length; t_word++) {
            if (t2s[t_word] != null) {
                for (Integer s_word : t2s[t_word]) {
                    if (!currentpoints[s_word][t_word]) {
                        if (unaligned_s.contains(s_word) && unaligned_t.contains(t_word)) {
                            currentpoints[s_word][t_word] = true;
                            unaligned_s.remove(s_word);
                            unaligned_t.remove(t_word);
                            break;
                        }
                    }
                }
            }
        }
    }

}
