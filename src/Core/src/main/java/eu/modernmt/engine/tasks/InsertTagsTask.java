package eu.modernmt.engine.tasks;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.fastalign.ForceTranslation;
import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.engine.SlaveNode;
import eu.modernmt.model.AutomaticTaggedTranslation;
import eu.modernmt.model.Sentence;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.xml.XMLTagMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by luca mastrostefano on 09/12/15.
 */
public class InsertTagsTask extends DistributedCallable<AutomaticTaggedTranslation> {

    private static final Logger logger = LogManager.getLogger(InsertTagsTask.class);
    private static final boolean PROCESSING_ENABLED = true;
    private static final XMLTagMapper tagMapper = new XMLTagMapper();
    static private Preprocessor targetPreprocessor = null;

    private final String sentence_str;
    private final String translation_str;
    private final boolean forceTranslation;
    private final Symmetrisation.Strategy symmetrizationStrategy;

    public InsertTagsTask(String sentence, String translation, boolean forceTranslation) {
        this(sentence, translation, forceTranslation, null);
    }

    public InsertTagsTask(String sentence, String translation, boolean forceTranslation,
                          Symmetrisation.Strategy symmetrizationStrategy) {
        this.sentence_str = sentence;
        this.translation_str = translation;
        this.forceTranslation = forceTranslation;
        this.symmetrizationStrategy = symmetrizationStrategy;
    }

    @Override
    public SlaveNode getWorker() {
        return (SlaveNode) super.getWorker();
    }

    @Override
    public AutomaticTaggedTranslation call() throws ProcessingException {
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

            AutomaticTaggedTranslation automaticTaggedTranslation = new AutomaticTaggedTranslation(
                    preprocessedTranslation.getWords(), preprocessedSentence, alignments);

            tagMapper.call(automaticTaggedTranslation);

            startTime = System.currentTimeMillis();
            String taggedTranslation;
            if (forceTranslation) {
                taggedTranslation = ForceTranslation.forceTranslationAndPreserveTags(automaticTaggedTranslation, this.translation_str);
            } else {
                taggedTranslation = automaticTaggedTranslation.toString();
            }
            automaticTaggedTranslation.setAutomaticTaggedTranslation(taggedTranslation);
            endTime = System.currentTimeMillis();

            if (logger.isDebugEnabled()) {
                logger.debug("Time for forcing the translation: " + (endTime - startTime) + " [ms]");
                logger.debug("Total time for tags projection: " + (endTime - beginTime) + " [ms]");
            }

            return automaticTaggedTranslation;
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

}
