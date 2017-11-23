package eu.modernmt.facade;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.engine.Engine;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.xml.XMLTagProjector;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Created by davide on 20/04/16.
 */
public class TagFacade {

    public Translation project(LanguagePair direction, String sentence, String translation) throws AlignerException {
        return project(direction, sentence, translation, null);
    }

    public Translation project(LanguagePair direction, String sentence, String translation, Aligner.SymmetrizationStrategy strategy) throws AlignerException {
        LanguageIndex languages = ModernMT.getNode().getEngine().getLanguages();

        LanguagePair mappedDirection = languages.map(direction);
        if (mappedDirection == null) {
            // Aligner is always bi-directional even if the engine does not support it
            mappedDirection = languages.map(direction.reversed());

            if (mappedDirection == null)
                throw new UnsupportedLanguageException(direction);
            else
                mappedDirection = mappedDirection.reversed();
        }

        ProjectTagsCallable operation = new ProjectTagsCallable(mappedDirection, sentence, translation, strategy);
        try {
            return operation.call(); //run the callable operation locally, so do not submit it to an executor but just run its "call" method
        } catch (Throwable e) {
            if (e instanceof ProcessingException)
                throw new AlignerException("Problem while processing translation", e);
            else if (e instanceof AlignerException)
                throw new AlignerException("Problem while computing alignments", e);
            else if (e instanceof RuntimeException)
                throw new AlignerException("Unexpected exception while projecting tags", e);
            else
                throw new Error("Unexpected exception: " + e.getMessage(), e);
        }
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

            Preprocessor preprocessor = engine.getPreprocessor();

            Sentence sentence = preprocessor.process(direction, sentenceString);
            Sentence translation = preprocessor.process(direction.reversed(), translationString);

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
