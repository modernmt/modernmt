package eu.modernmt.engine.tasks;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.engine.SlaveNode;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.xml.XMLTagMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

/**
 * Created by luca mastrostefano on 09/12/15.
 */
public class InsertTagsTask extends DistributedCallable<Translation> {

    private static final Logger logger = LogManager.getLogger(InsertTagsTask.class);
    private static final boolean PROCESSING_ENABLED = true;
    private static final XMLTagMapper tagMapper = new XMLTagMapper();
    private static Preprocessor targetPreprocessor = null;
    public static final boolean DEFAULT_INVERTED = false;

    private String sentence_str;
    private String translation_str;
    private final Symmetrisation.Strategy symmetrizationStrategy;
    private final boolean inverted;

    public InsertTagsTask(String sentence, String translation) {
        this(sentence, translation, null, DEFAULT_INVERTED);
    }

    public InsertTagsTask(String sentence, String translation, boolean inverted) {
        this(sentence, translation, null, inverted);
    }

    public InsertTagsTask(String sentence, String translation,
                          Symmetrisation.Strategy symmetrizationStrategy) {
        this(sentence, translation, symmetrizationStrategy, DEFAULT_INVERTED);
    }

    public InsertTagsTask(String sentence, String translation,
                          Symmetrisation.Strategy symmetrizationStrategy, boolean inverted) {
        this.sentence_str = sentence;
        this.translation_str = translation;
        this.symmetrizationStrategy = symmetrizationStrategy;
        this.inverted = inverted;
    }

    @Override
    public SlaveNode getWorker() {
        return (SlaveNode) super.getWorker();
    }

    @Override
    public Translation call() throws ProcessingException {
        long beginTime = System.currentTimeMillis();
        long endTime;
        SlaveNode worker = getWorker();
        Aligner aligner = worker.getAligner();
        try {
            if (targetPreprocessor == null) {
                targetPreprocessor = new Preprocessor(getWorker().getEngine().getTargetLanguage());
            }
            if (this.inverted) {
                this.invertSentenceAndTranslation();
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

            if (this.inverted) {
                taggedTranslation = invertTranslation(taggedTranslation);
            }
            return taggedTranslation;
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    private void invertSentenceAndTranslation() {
        String tmp = this.sentence_str;
        this.sentence_str = this.translation_str;
        this.translation_str = tmp;
    }

    private static Translation invertTranslation(Translation translation) {
        return new Translation(
                translation.getSource().getWords(), translation.getTags(),
                translation, translation.getAlignment());
    }

    public static void main(String[] args) throws ProcessingException {
        try {
            String sentence = "It is often <i>*99***1#</i>.";
            String translation = "Spesso corrisponde a *99***1#.";
            Preprocessor sourcePreprocessor = new Preprocessor(Locale.forLanguageTag("en"));
            Preprocessor targetPreprocessor = new Preprocessor(Locale.forLanguageTag("it"));
            Sentence preprocessedSentence = sourcePreprocessor.process(sentence, true);
            Sentence preprocessedTranslation = targetPreprocessor.process(translation, true);

            String alignments_str = "[[0, 0], [1, 1], [2, 0], [3, 3], [4, 4], [5, 5], [6, 6], [7, 7], [8, 8], [9, 9], [10, 10]]";
            int[][] alignments = FastAlign.parseAlignments(alignments_str.substring(0, alignments_str.length() - 2).substring(2)
                    .replaceAll("\\], \\[", " ").replaceAll(", ", "-"));

            System.out.println("Source:\n" + sentence);
            System.out.println("Translation:\n" + translation);
            System.out.println("Alignments:\n" + alignments_str);

            Translation taggedTranslation = new Translation(
                    preprocessedTranslation.getWords(), preprocessedSentence, alignments);

            tagMapper.call(taggedTranslation, null);

            System.out.println("Tagged translation:\n" + taggedTranslation);
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

}
