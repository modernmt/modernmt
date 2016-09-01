package eu.modernmt.aligner;

import eu.modernmt.aligner.symal.GrowDiagonalFinalAndStrategy;
import eu.modernmt.aligner.symal.SymmetrizationStrategy;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import org.apache.commons.io.IOUtils;

import java.util.List;

/**
 * Created by davide on 09/05/16.
 */
public class SymmetrizedAligner implements Aligner {

    private final Aligner forwardModel;
    private final Aligner backwardModel;
    private SymmetrizationStrategy strategy;

    public SymmetrizedAligner(Aligner forwardModel, Aligner backwardModel) {
        this.forwardModel = forwardModel;
        this.backwardModel = backwardModel;
        this.strategy = new GrowDiagonalFinalAndStrategy();
    }

    @Override
    public Alignment getAlignment(Sentence source, Sentence target) throws AlignerException {
        Alignment forwardAlignments = forwardModel.getAlignment(source, target);
        Alignment backwardAlignments = backwardModel.getAlignment(source, target);

        return strategy.symmetrize(forwardAlignments, backwardAlignments);
    }

    @Override
    public Alignment[] getAlignments(List<Sentence> sources, List<Sentence> targets) throws AlignerException {
        Alignment[] forwardAlignments = forwardModel.getAlignments(sources, targets);
        Alignment[] backwardAlignments = backwardModel.getAlignments(sources, targets);

        Alignment[] alignments = new Alignment[forwardAlignments.length];

        for (int i = 0; i < alignments.length; i++)
            alignments[i] = strategy.symmetrize(forwardAlignments[i], backwardAlignments[i]);

        return alignments;
    }

    public void setSymmetrizationStrategy(SymmetrizationStrategy strategy) {
        if (strategy == null)
            throw new NullPointerException();

        this.strategy = strategy;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(forwardModel);
        IOUtils.closeQuietly(backwardModel);
    }

}
