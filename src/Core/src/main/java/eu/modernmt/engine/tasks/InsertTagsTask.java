package eu.modernmt.engine.tasks;

import eu.modernmt.Aligner;
import eu.modernmt.ForceTranslation;
import eu.modernmt.engine.SlaveNode;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.framework.ProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

/**
 * Created by luca mastrostefano on 09/12/15.
 */
public class InsertTagsTask extends DistributedCallable<String> {

    private static final Logger logger = LogManager.getLogger(InsertTagsTask.class);
    private static final boolean processingEnabled = true;
    private final String sentence_str;
    private final String translation_str;

    public InsertTagsTask(String sentence, String translation) {
        this.sentence_str = sentence;
        this.translation_str = translation;
    }

    @Override
    public SlaveNode getWorker() {
        return (SlaveNode) super.getWorker();
    }

    @Override
    public String call() throws ProcessingException {
        long beginTime = System.currentTimeMillis();
        long startTime, endTime;
        SlaveNode worker = getWorker();
        Aligner aligner = worker.getAligner();
        try {
            Sentence preprocessedSentence = worker.getPreprocessor().process(this.sentence_str, this.processingEnabled);
            Sentence preprocessedTranslation = worker.getPreprocessor().process(this.translation_str, this.processingEnabled);

            logger.debug("Tokens prima dell'aligner: " + Arrays.toString(preprocessedTranslation.getWords()) + "  "  + Arrays.toString(preprocessedTranslation.getTags()));

            startTime = System.currentTimeMillis();
            int[][] alignments = aligner.getAlignments(preprocessedSentence, preprocessedTranslation);
            endTime = System.currentTimeMillis();
            logger.debug("Time for getting the alignments: " + (endTime - startTime) + " [ms]");

            logger.debug("Tokens dopo l'aligner: " + Arrays.toString(preprocessedTranslation.getWords()) + "  "  + Arrays.toString(preprocessedTranslation.getTags()));
            Translation translation = new Translation(preprocessedTranslation.getWords(),
                    preprocessedSentence, alignments);


            Postprocessor postprocessor = worker.getPostprocessor();
            postprocessor.process(translation, this.processingEnabled);

            logger.debug("Token dopo il tag mapper: " + Arrays.toString(translation.getWords()) + "  "  + Arrays.toString(translation.getTags()));

            startTime = System.currentTimeMillis();
            ForceTranslation.forceTranslation(translation_str, translation);
            logger.debug("Token dopo la forzatura: " + Arrays.toString(translation.getWords()) + "  "  + Arrays.toString(translation.getTags()));
            endTime = System.currentTimeMillis();
            logger.debug("Time for forcing the translation: " + (endTime - startTime) + " [ms]");
            logger.debug("Total time for tags projection: " + (endTime - beginTime) + " [ms]");

            return  translation.toString();
        }catch(Exception e){
            throw new ProcessingException(e);
        }
    }

}
