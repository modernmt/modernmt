package eu.modernmt.engine.tasks;

import eu.modernmt.Aligner;
import eu.modernmt.ForceTranslation;
import eu.modernmt.engine.SlaveNode;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.framework.ProcessingException;

/**
 * Created by luca mastrostefano on 09/12/15.
 */
public class InsertTagsTask extends DistributedCallable<String> {

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
        SlaveNode worker = getWorker();
        Aligner aligner = worker.getAligner();
        try {
            Sentence preprocessedSentence = worker.getPreprocessor().process(this.sentence_str, this.processingEnabled);
            Sentence preprocessedTranslation = worker.getPreprocessor().process(this.translation_str, this.processingEnabled);

            int[][] alignments = aligner.getAlignments(preprocessedSentence, preprocessedTranslation);

            Translation translation = new Translation(preprocessedTranslation.getWords(),
                    preprocessedSentence, alignments);

            Postprocessor postprocessor = worker.getPostprocessor();
            postprocessor.process(translation, this.processingEnabled);

            ForceTranslation.forceTranslation(translation_str, translation);
            return  translation.toString();
        }catch(Exception e){
            throw new ProcessingException(e);
        }
    }

}
