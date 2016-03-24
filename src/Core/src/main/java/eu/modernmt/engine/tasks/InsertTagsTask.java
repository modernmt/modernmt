package eu.modernmt.engine.tasks;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.engine.SlaveNode;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.framework.ProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

            startTime = System.currentTimeMillis();
            int[][] alignments = aligner.getAlignments(preprocessedSentence, preprocessedTranslation);
            endTime = System.currentTimeMillis();

            Translation translation = new Translation(preprocessedTranslation.getWords(),
                    preprocessedSentence, alignments);


            Postprocessor postprocessor = worker.getPostprocessor();
            postprocessor.process(translation, this.processingEnabled);
            
            startTime = System.currentTimeMillis();
            String taggedTranslation = translation.toString();
            //String taggedTranslation = ForceTranslation.forceTranslationAndPreserveTags(translation, this.translation_str);
            endTime = System.currentTimeMillis();
            logger.debug("Time for forcing the translation: " + (endTime - startTime) + " [ms]");
            logger.debug("Total time for tags projection: " + (endTime - beginTime) + " [ms]");

            return  taggedTranslation;
        }catch(Exception e){
            throw new ProcessingException(e);
        }
    }

}
