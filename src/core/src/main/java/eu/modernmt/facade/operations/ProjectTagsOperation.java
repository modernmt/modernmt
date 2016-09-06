package eu.modernmt.facade.operations;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.engine.Engine;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.xml.XMLTagProjector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by davide on 22/04/16.
 */
public class ProjectTagsOperation extends Operation<Translation> {

    private static final Logger logger = LogManager.getLogger(ProjectTagsOperation.class);
    private static final XMLTagProjector tagProjector = new XMLTagProjector();

    private String sentenceString;
    private String translationString;
    private final Aligner.SymmetrizationStrategy strategy;
    private final boolean inverted;

    public ProjectTagsOperation(String sentence, String translation, boolean inverted, Aligner.SymmetrizationStrategy strategy) {
        this.sentenceString = sentence;
        this.translationString = translation;
        this.strategy = strategy;
        this.inverted = inverted;
    }

    @Override
    public Translation call() throws ProcessingException, AlignerException {
        Engine engine = getEngine();
        Aligner aligner = engine.getAligner();

        Preprocessor sourcePreprocessor = engine.getSourcePreprocessor();
        Preprocessor targetPreprocessor = engine.getTargetPreprocessor();

        long beginTime = System.currentTimeMillis();
        long endTime;

        String sentenceString = this.inverted ? this.translationString : this.sentenceString;
        String translationString = this.inverted ? this.sentenceString : this.translationString;

        Sentence sentence = sourcePreprocessor.process(sentenceString);
        Sentence translation = targetPreprocessor.process(translationString);

        Alignment alignment;

        if (strategy != null)
            alignment = aligner.getAlignment(sentence, translation, strategy);
        else
            alignment = aligner.getAlignment(sentence, translation);

        if (this.inverted) {
            alignment = alignment.getInverse();
            Sentence tmp = sentence;
            sentence = translation;
            translation = tmp;
        }

        Translation taggedTranslation = new Translation(translation.getWords(), sentence, alignment);
        tagProjector.project(taggedTranslation);

        endTime = System.currentTimeMillis();
        if (logger.isDebugEnabled()) {
            logger.debug("Total time for tags projection: " + (endTime - beginTime) + " [ms]");
        }

        return taggedTranslation;
    }

}
