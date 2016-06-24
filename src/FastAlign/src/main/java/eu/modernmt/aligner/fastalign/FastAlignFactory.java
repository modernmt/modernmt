package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.aligner.AlignerFactory;
import eu.modernmt.aligner.SymmetrizedAligner;
import eu.modernmt.io.Paths;

import java.io.File;

/**
 * Created by davide on 09/05/16.
 */
public class FastAlignFactory extends AlignerFactory {

    private static final String FORWARD_MODEL_NAME = "model.align.fwd";
    private static final String BACKWARD_MODEL_NAME = "model.align.bwd";

    @Override
    public Aligner create() throws AlignerException {
        File modelDirectory = Paths.join(enginePath, "models", "phrase_tables");
        File fwdModelFile = new File(modelDirectory, FORWARD_MODEL_NAME);
        File bwdModelFile = new File(modelDirectory, BACKWARD_MODEL_NAME);

        FastAlign fwdModel = new FastAlign(fwdModelFile, false);
        FastAlign bwdModel = new FastAlign(bwdModelFile, true);

        return new SymmetrizedAligner(fwdModel, bwdModel);
    }

}
