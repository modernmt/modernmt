package eu.modernmt;

import eu.modernmt.model.Sentence;
import eu.modernmt.symal.Symmetrisation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public class SymmetrizedAligner implements Aligner{

    private static final String forwardModelFileName = "model.align.fwd";
    private static final String backwardModelFileName = "model.align.bwd";
    private static final Logger logger = LogManager.getLogger(SymmetrizedAligner.class);

    private static class AlignerInitializer implements Runnable{

        private Aligner aligner;

        public AlignerInitializer(Aligner aligner) {
            this.aligner = aligner;
        }

        @Override
        public void run() {
            try {
                this.aligner.init();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

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
    public void init() {
        try {
            logger.info("Initializing Fast Align");
            Thread forwardModelInitializer = new Thread(new AlignerInitializer(this.forwardAlignerProcess));
            Thread backwardModelInitializer = new Thread(new AlignerInitializer(this.backwardAlignerProcess));
            logger.info("Loading forward model");
            forwardModelInitializer.start();
            logger.info("Loading backward model");
            backwardModelInitializer.start();
            forwardModelInitializer.join();
            backwardModelInitializer.join();
            logger.info("Fast Align initialized");
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized int[][] getAlignments(Sentence  sentence, Sentence translation) throws IOException {
        String forwardAlignments = this.forwardAlignerProcess.getStringAlignments(sentence, translation);
        String backwardAlignments = this.backwardAlignerProcess.getStringAlignments(sentence, translation);
        logger.debug("Symmetrising");
        String symmetrisedAlignments = Symmetrisation.symmetriseMosesFormatAlignment(forwardAlignments,
                backwardAlignments, Symmetrisation.Type.GrowDiagFinalAnd);
        logger.debug("Symmetrised alignments: " + symmetrisedAlignments);
        return Aligner.parseAlignments(symmetrisedAlignments);
    }

    @Override
    public void close() throws IOException {
        this.forwardAlignerProcess.close();
        this.backwardAlignerProcess.close();
    }

}
