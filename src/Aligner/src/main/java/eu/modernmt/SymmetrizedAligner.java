package eu.modernmt;

import eu.modernmt.model.Sentence;
import eu.modernmt.symal.Symmetrisation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public class SymmetrizedAligner implements Aligner{

    private static final String forwardModelFileName = "model.align.fwd";
    private static final String backwardModelFileName = "model.align.bwd";
    private static final Logger logger = LogManager.getLogger(SymmetrizedAligner.class);

    private final FastAlign forwardAlignerProcess;
    private final FastAlign backwardAlignerProcess;

    public SymmetrizedAligner(String enginePath){
        File modelsFile = new File(enginePath,
                "models" + File.separatorChar + "phrase_tables");
        String forwardModelFilePath = new File(modelsFile.getAbsolutePath(), forwardModelFileName).getAbsolutePath();
        String backwardModelFilePath = new File(modelsFile.getAbsolutePath(), backwardModelFileName).getAbsolutePath();

        forwardAlignerProcess = new FastAlign(false, forwardModelFilePath);
        backwardAlignerProcess = new FastAlign(true, backwardModelFilePath);
    }

    @Override
    public void init() throws IOException, ParseException {
        logger.info("Initializing Fast Align");

        try {
            logger.info("Loading forward model");
            this.forwardAlignerProcess.init();
            logger.info("Loading backward model");
            this.backwardAlignerProcess.init();
        }catch(Exception e){
            this.close();
            throw e;
        }
        logger.info("Fast Align initialized");
    }

    @Override
    public synchronized int[][] getAlignments(Sentence  sentence, Sentence translation) throws IOException {
        String forwardAlignemnts = this.forwardAlignerProcess.getStringAlignments(sentence, translation);
        String backwardAlignemnts = this.backwardAlignerProcess.getStringAlignments(sentence, translation);
        logger.debug("Symmetrising");
        String symmetrisedAlignments = Symmetrisation.symmetriseMosesFormatAlignment(forwardAlignemnts,
                backwardAlignemnts, Symmetrisation.Type.GrowDiagFinalAnd);
        logger.debug("Symmetrised alignments: " + symmetrisedAlignments);
        return Aligner.parseAlignments(symmetrisedAlignments);
    }

    @Override
    public void close() throws IOException {
        this.forwardAlignerProcess.close();
        this.backwardAlignerProcess.close();
    }

}
