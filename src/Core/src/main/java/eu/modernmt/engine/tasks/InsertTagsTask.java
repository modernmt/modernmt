package eu.modernmt.engine.tasks;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.engine.SlaveNode;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.xml.New_XMLTagMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by luca mastrostefano on 09/12/15.
 */
public class InsertTagsTask extends DistributedCallable<Translation> {

    private static final Logger logger = LogManager.getLogger(InsertTagsTask.class);
    private static final boolean PROCESSING_ENABLED = true;
    private static final New_XMLTagMapper tagMapper = new New_XMLTagMapper();
    static private Preprocessor targetPreprocessor = null;

    private final String sentence_str;
    private final String translation_str;
    private final Symmetrisation.Strategy symmetrizationStrategy;

    public InsertTagsTask(String sentence, String translation) {
        this(sentence, translation, null);
    }

    public InsertTagsTask(String sentence, String translation,
                          Symmetrisation.Strategy symmetrizationStrategy) {
        this.sentence_str = sentence;
        this.translation_str = translation;
        this.symmetrizationStrategy = symmetrizationStrategy;
    }

    @Override
    public SlaveNode getWorker() {
        return (SlaveNode) super.getWorker();
    }

    @Override
    public Translation call() throws ProcessingException {
        long beginTime = System.currentTimeMillis();
        long startTime, endTime;
        SlaveNode worker = getWorker();
        Aligner aligner = worker.getAligner();
        try {
            if (targetPreprocessor == null) {
                targetPreprocessor = new Preprocessor(getWorker().getEngine().getTargetLanguage());
            }
            Sentence preprocessedSentence = worker.getPreprocessor().process(this.sentence_str, PROCESSING_ENABLED);
            Sentence preprocessedTranslation = targetPreprocessor.process(this.translation_str, PROCESSING_ENABLED);

            if (this.symmetrizationStrategy != null) {
                aligner.setSymmetrizationStrategy(this.symmetrizationStrategy);
            }

            int[][] alignments = aligner.getAlignments(preprocessedSentence, preprocessedTranslation);

            Translation taggedTranslation = new Translation(
                    preprocessedTranslation.getWords(), preprocessedSentence, alignments);

            tagMapper.call(taggedTranslation, null);

            endTime = System.currentTimeMillis();
            if (logger.isDebugEnabled()) {
                logger.debug("Total time for tags projection: " + (endTime - beginTime) + " [ms]");
            }

            return taggedTranslation;
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

}
