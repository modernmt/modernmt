package eu.modernmt.aligner.symal;

/**
 * Created by davide on 20/05/16.
 */
public class UnionStrategy implements SymmetrizationStrategy {

    @Override
    public int[][] symmetrize(int[][] forward, int[][] backward) {
        return AlignmentMatrix.build(forward, backward)
                .or(forward)
                .or(backward)
                .toArray();
    }

}
