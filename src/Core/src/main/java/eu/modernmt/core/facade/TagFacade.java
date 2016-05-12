package eu.modernmt.core.facade;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.aligner.symal.Symmetrization;
import eu.modernmt.core.cluster.error.SystemShutdownException;
import eu.modernmt.core.facade.error.TranslationException;
import eu.modernmt.core.facade.operations.ProjectTagsOperation;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.framework.ProcessingException;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 20/04/16.
 */
public class TagFacade {

    public Translation project(String sentence, String translation, Locale sourceLanguage, Locale targetLanguage) throws TranslationException {
        return project(sentence, translation, sourceLanguage, targetLanguage, null);
    }

    public Translation project(String sentence, String translation, Locale sourceLanguage, Locale targetLanguage, Symmetrization.Strategy symmetrizationStrategy) throws TranslationException {
        ProjectTagsOperation operation = new ProjectTagsOperation(sentence, translation, sourceLanguage, targetLanguage, symmetrizationStrategy);

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
