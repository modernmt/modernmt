package eu.modernmt.aligner.symal;

import eu.modernmt.model.Alignment;

import java.util.Arrays;

/**
 * Created by davide on 20/05/16.
 */
public class GrowDiagonalFinalAndStrategy implements SymmetrizationStrategy {

    @Override
    public Alignment symmetrize(Alignment forward, Alignment backward) {
        AlignmentMatrix intersect = AlignmentMatrix.build(forward, backward)
                .or(forward)
                .and(backward);
        AlignmentMatrix union = AlignmentMatrix.build(forward, backward)
                .or(forward)
                .or(backward);

        intersect = GrowDiagonalStrategy.symmetrize(intersect, union);

        // Forward final and
        for (int s = 0; s < intersect.getForwardSize(); s++) {
            if (intersect.isSourceWordAligned(s))
                continue;

            int[] targets = forward.getWordsAlignedWithSource(s);
            Arrays.sort(targets);

            for (int t : targets) {
                if (!intersect.isTargetWordAligned(t)) {
                    intersect.set(s, t);
                    break;
                }
            }
        }

        // Backward final and
        for (int t = 0; t < intersect.getBackwardSize(); t++) {
            if (intersect.isTargetWordAligned(t))
                continue;

            int[] sources = backward.getWordsAlignedWithTarget(t);
            Arrays.sort(sources);

            for (int s : sources) {
                if (!intersect.isSourceWordAligned(s)) {
                    intersect.set(s, t);
                    break;
                }
            }
        }

        return intersect.toAlignment();
    }

}
