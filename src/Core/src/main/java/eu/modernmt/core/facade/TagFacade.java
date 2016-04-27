package eu.modernmt.core.facade;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.core.cluster.error.SystemShutdownException;
import eu.modernmt.core.facade.error.TranslationException;
import eu.modernmt.core.facade.operations.ProjectTagsOperation;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.framework.ProcessingException;

import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 20/04/16.
 */
public class TagFacade {

    public Translation project(String sentence, String translation) throws TranslationException {
        return project(sentence, translation, null, false);
    }

    public Translation project(String sentence, String translation, boolean inverted) throws TranslationException {
        return project(sentence, translation, null, inverted);
    }

    public Translation project(String sentence, String translation, Symmetrisation.Strategy symmetrizationStrategy) throws TranslationException {
        return project(sentence, translation, symmetrizationStrategy, false);
    }

    public Translation project(String sentence, String translation, Symmetrisation.Strategy symmetrizationStrategy, boolean invert) throws TranslationException {
        ProjectTagsOperation operation = new ProjectTagsOperation(sentence, translation, symmetrizationStrategy, invert);

        try {
            return ModernMT.node.submit(operation).get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof ProcessingException)
                throw new TranslationException("Problem while processing translation", cause);
            else if (cause instanceof AlignerException)
                throw new TranslationException("Problem while computing alignments", cause);
            else if (cause instanceof RuntimeException)
                throw new TranslationException("Unexpected error while projecting tags", cause);
            else
                throw new Error("Unexpected exception: " + cause.getMessage(), cause);
        }
    }

}
