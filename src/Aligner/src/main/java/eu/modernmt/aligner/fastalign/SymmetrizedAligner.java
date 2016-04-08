package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.model.Sentence;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public class SymmetrizedAligner implements Aligner {

    private static final String forwardModelFileName = "model.align.fwd";
    private static final String backwardModelFileName = "model.align.bwd";
    private static final Logger logger = LogManager.getLogger(eu.modernmt.aligner.fastalign.SymmetrizedAligner.class);
    public static final Symmetrisation.Strategy DEFAULT_SYMMETRIZATION_STRATEGY = Symmetrisation.Strategy.GrowDiagFinalAnd;

    private static class AlignerInitializer implements Runnable {

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
    private Symmetrisation.Strategy simmetrizationStrategy;

    public SymmetrizedAligner(String enginePath) {
        File modelsFile = new File(enginePath,
                "models" + File.separatorChar + "phrase_tables");
        String forwardModelFilePath = new File(modelsFile.getAbsolutePath(), forwardModelFileName).getAbsolutePath();
        String backwardModelFilePath = new File(modelsFile.getAbsolutePath(), backwardModelFileName).getAbsolutePath();

        forwardAlignerProcess = new FastAlign(false, forwardModelFilePath);
        backwardAlignerProcess = new FastAlign(true, backwardModelFilePath);
        this.simmetrizationStrategy = DEFAULT_SYMMETRIZATION_STRATEGY;
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setSymmetrizationStrategy(Symmetrisation.Strategy strategy) {
        this.simmetrizationStrategy = strategy;
    }

    @Override
    public synchronized int[][] getAlignments(Sentence sentence, Sentence translation) throws IOException {
        String forwardAlignments = this.forwardAlignerProcess.getStringAlignments(sentence, translation);
        String backwardAlignments = this.backwardAlignerProcess.getStringAlignments(sentence, translation);
        String invertedBackwardAlignments = eu.modernmt.aligner.fastalign.SymmetrizedAligner.invertAlignments(backwardAlignments);
        logger.debug("Symmetrising");
        String symmetrisedAlignments = Symmetrisation.symmetriseMosesFormatAlignment(forwardAlignments,
                invertedBackwardAlignments, this.simmetrizationStrategy);
        logger.debug("Symmetrised alignments: " + symmetrisedAlignments);
        return FastAlign.parseAlignments(symmetrisedAlignments);
    }

    private static String invertAlignments(String stringAlignments) {
        String[] links_str = stringAlignments.split(" ");
        StringBuilder invertedAlignments = new StringBuilder();
        for (int i = 0; i < links_str.length; i++) {
            String[] alignment = links_str[i].split("-");
            invertedAlignments.append(alignment[1] + "-" + alignment[0] + " ");
        }
        return invertedAlignments.deleteCharAt(invertedAlignments.length() - 1).toString();
    }

    @Override
    public void close() throws IOException {
        this.forwardAlignerProcess.close();
        this.backwardAlignerProcess.close();
    }

}
