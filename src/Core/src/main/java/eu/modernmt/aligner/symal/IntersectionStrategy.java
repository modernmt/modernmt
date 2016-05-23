package eu.modernmt.aligner.symal;

/**
 * Created by davide on 20/05/16.
 */
public class IntersectionStrategy implements SymmetrizationStrategy {

    @Override
    public int[][] symmetrize(int[][] forward, int[][] backward) {
        return AlignmentMatrix.build(forward, backward)
                .or(forward)
                .and(backward)
                .toArray();
    }

}
