package eu.modernmt.facade;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.engine.Engine;
import eu.modernmt.model.*;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.xml.XMLTagProjector;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 20/04/16.
 */
public class TagFacade {

    public Translation project(LanguagePair direction, String sentence, String translation) throws AlignerException {
        return project(direction, sentence, translation, null);
    }

    public Translation project(LanguagePair direction, String sentence, String translation, Aligner.SymmetrizationStrategy strategy) throws AlignerException {
        ensureLanguagePairIsSupported(direction);

        ProjectTagsCallable operation = new ProjectTagsCallable(direction, sentence, translation, strategy);

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

    private void ensureLanguagePairIsSupported(LanguagePair pair) {
        Engine engine = ModernMT.getNode().getEngine();

        // Aligner is always bi-directional even if the engine does not support it
        if (!engine.isLanguagePairSupported(pair) && !engine.isLanguagePairSupported(pair))
            throw new UnsupportedLanguageException(pair);
    }

    private static class ProjectTagsCallable implements Callable<Translation>, Serializable {

        private static final XMLTagProjector tagProjector = new XMLTagProjector();

        private final LanguagePair direction;
        private final String sentenceString;
        private final String translationString;
        private final Aligner.SymmetrizationStrategy strategy;

        public ProjectTagsCallable(LanguagePair direction, String sentence, String translation, Aligner.SymmetrizationStrategy strategy) {
            this.direction = direction;
            this.sentenceString = sentence;
            this.translationString = translation;
            this.strategy = strategy;
        }

        @Override
        public Translation call() throws ProcessingException, AlignerException {
            ClusterNode node = ModernMT.getNode();
            Engine engine = node.getEngine();
            Aligner aligner = engine.getAligner();

            Preprocessor sourcePreprocessor = engine.getPreprocessor(direction);
            Preprocessor targetPreprocessor = engine.getPreprocessor(direction.reversed());

            Sentence sentence = sourcePreprocessor.process(sentenceString);
            Sentence translation = targetPreprocessor.process(translationString);

            Alignment alignment;

            if (strategy != null)
                alignment = aligner.getAlignment(direction, sentence, translation, strategy);
            else
                alignment = aligner.getAlignment(direction, sentence, translation);

            Translation taggedTranslation = new Translation(translation.getWords(), sentence, alignment);
            tagProjector.project(taggedTranslation);

            return taggedTranslation;
        }

    }

}
