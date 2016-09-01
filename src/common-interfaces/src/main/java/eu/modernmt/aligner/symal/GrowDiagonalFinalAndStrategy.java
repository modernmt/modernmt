package eu.modernmt.aligner.symal;

import java.util.Arrays;

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

}
