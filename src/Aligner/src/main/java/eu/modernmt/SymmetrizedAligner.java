package eu.modernmt;

import eu.modernmt.config.Config;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.util.TokensOutputter;
import eu.modernmt.symal.Symmetrisation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.text.ParseException;

/**
 * Created by lucamastrostefano on 14/03/16.
 */
public class SymmetrizedAligner implements Aligner{

    private static final String SENTENCE_SEPARATOR = " ||| ";
    private static final Logger logger = LogManager.getLogger(SymmetrizedAligner.class);

    private final AlignerProcess forwardAlignerProcess;
    private final AlignerProcess backwardAlignerProcess;

    public SymmetrizedAligner(String enginePath){
        forwardAlignerProcess = new AlignerProcess(false, enginePath);
        backwardAlignerProcess = new AlignerProcess(true, enginePath);
    }

    @Override
    public void init() throws IOException, ParseException {
        logger.info("Initializing Fast Align");

        try {
            logger.info("Loading forward model");
            this.forwardAlignerProcess.run();
            logger.info("Loading backward model");
            this.backwardAlignerProcess.run();
        }catch(Exception e){
            this.close();
            throw e;
        }
        logger.info("Fast Align initialized");
    }

    @Override
    public synchronized int[][] getAlignments(Sentence  sentence, Sentence translation) throws IOException {
        String sentence_str = TokensOutputter.toString(sentence, false, false);
        String translation_str = TokensOutputter.toString(translation, false, false);
        String forwardQuery = sentence_str + SENTENCE_SEPARATOR + translation_str + "\n";
        logger.debug("Sending query to Fast Align's models: " + forwardQuery);
        this.forwardAlignerProcess.standardInput.write(forwardQuery.getBytes(Config.charset.get()));
        this.forwardAlignerProcess.standardInput.flush();
        String backwardQuery = translation_str + SENTENCE_SEPARATOR + sentence_str + "\n";
        this.backwardAlignerProcess.standardInput.write(backwardQuery.getBytes(Config.charset.get()));
        this.backwardAlignerProcess.standardInput.flush();
        logger.debug("Waiting for alignments");
        String forwardModelResponse = this.forwardAlignerProcess.standardOutput.readLine();
        logger.debug("Forward alignments: " + forwardModelResponse);
        String backwardModelResponse = this.backwardAlignerProcess.standardOutput.readLine();
        logger.debug("Backward alignments: " + backwardModelResponse);
        logger.debug("Symmetrising");
        String symmetrisedAlignments = Symmetrisation.symmetriseMosesFormatAlignment(forwardModelResponse,
                backwardModelResponse, Symmetrisation.Type.GrowDiagFinalAnd);
        logger.debug("Symmetrised alignments: " + symmetrisedAlignments);
        String[] links_str = symmetrisedAlignments.split(" ");
        int[][] alignments = new int[links_str.length][];
        for(int i = 0; i < links_str.length; i++){
            String[] alignment = links_str[i].split("-");
            alignments[i] = new int[]{Integer.parseInt(alignment[0]), Integer.parseInt(alignment[1])};
        }
        return alignments;
    }

    @Override
    public void close() throws IOException {
        this.forwardAlignerProcess.close();
        this.backwardAlignerProcess.close();
    }

}
