package eu.modernmt.facade;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.engine.Engine;
import eu.modernmt.facade.exceptions.validation.LanguagePairNotSupportedException;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Languages;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.xml.XMLTagProjector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 20/04/16.
 */
public class TagFacade {

    public Translation project(String sentence, String translation, Locale sourceLanguage, Locale targetLanguage) throws AlignerException, LanguagePairNotSupportedException {
        return project(sentence, translation, sourceLanguage, targetLanguage, null);
    }

    public Translation project(String sentence, String translation, Locale sourceLanguage, Locale targetLanguage, Aligner.SymmetrizationStrategy strategy) throws AlignerException, LanguagePairNotSupportedException {
        boolean inverted = isLanguagesInverted(sourceLanguage, targetLanguage);
        ProjectTagsCallable operation = new ProjectTagsCallable(sentence, translation, inverted, strategy);

        try {
            return ModernMT.getNode().submit(operation).get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof ProcessingException)
                throw new AlignerException("Problem while processing translation", cause);
            else if (cause instanceof AlignerException)
                throw new AlignerException("Problem while computing alignments", cause);
            else if (cause instanceof RuntimeException)
                throw new AlignerException("Unexpected exception while projecting tags", cause);
            else
                throw new Error("Unexpected exception: " + cause.getMessage(), cause);
        }
    }

    public boolean isLanguagesSupported(Locale sourceLanguage, Locale targetLanguage)
            throws LanguagePairNotSupportedException {
        isLanguagesInverted(sourceLanguage, targetLanguage);
        return true;
    }


    private static boolean isLanguagesInverted(Locale sourceLanguage, Locale targetLanguage)
            throws LanguagePairNotSupportedException {
        Engine engine = ModernMT.getNode().getEngine();

        if (Languages.sameLanguage(engine.getSourceLanguage(), sourceLanguage) &&
                Languages.sameLanguage(engine.getTargetLanguage(), targetLanguage)) {
            return false;
        } else if (Languages.sameLanguage(engine.getSourceLanguage(), targetLanguage) &&
                Languages.sameLanguage(engine.getTargetLanguage(), sourceLanguage)) {
            return true;
        }

        throw new LanguagePairNotSupportedException(sourceLanguage, targetLanguage);
    }

    private static class ProjectTagsCallable implements Callable<Translation>, Serializable {

        private static final Logger logger = LogManager.getLogger(ProjectTagsCallable.class);
        private static final XMLTagProjector tagProjector = new XMLTagProjector();

        private final String sentenceString;
        private final String translationString;
        private final Aligner.SymmetrizationStrategy strategy;
        private final boolean inverted;

        public ProjectTagsCallable(String sentence, String translation, boolean inverted, Aligner.SymmetrizationStrategy strategy) {
            this.sentenceString = sentence;
            this.translationString = translation;
            this.strategy = strategy;
            this.inverted = inverted;
        }

        @Override
        public Translation call() throws ProcessingException, AlignerException {
            ClusterNode node = ModernMT.getNode();
            Engine engine = node.getEngine();
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

}
