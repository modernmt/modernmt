package eu.modernmt.aligner.symal;

import eu.modernmt.model.Alignment;

/**
 * Created by davide on 20/05/16.
 */
public class UnionStrategy implements SymmetrizationStrategy {

    @Override
    public Alignment symmetrize(Alignment forward, Alignment backward) {
        return AlignmentMatrix.build(forward, backward)
                .or(forward)
                .or(backward)
                .toAlignment();
    }

}
