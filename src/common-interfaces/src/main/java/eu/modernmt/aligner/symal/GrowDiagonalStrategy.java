package eu.modernmt.aligner.symal;

/**
 * Created by davide on 20/05/16.
 */
public class GrowDiagonalStrategy implements SymmetrizationStrategy {


    @Override
    public int[][] symmetrize(int[][] forward, int[][] backward) {
        AlignmentMatrix intersect = AlignmentMatrix.build(forward, backward)
                .or(forward)
                .and(backward);
        AlignmentMatrix union = AlignmentMatrix.build(forward, backward)
                .or(forward)
                .or(backward);


        return symmetrize(intersect, union).toArray();
    }

    static AlignmentMatrix symmetrize(AlignmentMatrix intersect, AlignmentMatrix union) {
        int fsize = intersect.getForwardSize();
        int bsize = intersect.getBackwardSize();

        boolean added = true;

        while (added) {
            added = false;

            // For all the current alignment
            for (int f = 0; f < fsize; f++) {
                for (int b = 0; b < bsize; b++) {

                    // If the word is aligned, the neighbours are checked
                    if (intersect.get(f, b)) {
                        for (int df = -1; df < 2; df++) {
                            for (int db = -1; db < 2; db++) {
                                if (df == 0 && db == 0)
                                    continue;

                                int pf = f + df;
                                int pb = b + db;

                                if (pf < 0 || pf >= fsize || pb < 0 || pb >= bsize)
                                    continue;

                                if (union.get(pf, pb) && (!intersect.isSourceWordAligned(pf) || !intersect.isTargetWordAligned(pb))) {
                                    intersect.set(pf, pb);
                                    added = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return intersect;
    }

}
