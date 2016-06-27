package eu.modernmt.core.facade;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.aligner.symal.SymmetrizationStrategy;
import eu.modernmt.core.Engine;
import eu.modernmt.core.cluster.error.SystemShutdownException;
import eu.modernmt.core.facade.exceptions.validation.LanguagePairNotSupportedException;
import eu.modernmt.core.facade.operations.ProjectTagsOperation;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.Languages;
import eu.modernmt.processing.framework.ProcessingException;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 20/04/16.
 */
public class TagFacade {

    public Translation project(String sentence, String translation, Locale sourceLanguage, Locale targetLanguage) throws AlignerException, LanguagePairNotSupportedException {
        return project(sentence, translation, sourceLanguage, targetLanguage, null);
    }

    public Translation project(String sentence, String translation, Locale sourceLanguage, Locale targetLanguage, SymmetrizationStrategy strategy) throws AlignerException, LanguagePairNotSupportedException {
        boolean inverted = isLanguagesInverted(sourceLanguage, targetLanguage);
        ProjectTagsOperation operation = new ProjectTagsOperation(sentence, translation, inverted, strategy);
        try {
            return ModernMT.node.submit(operation).get();
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
        Engine engine = ModernMT.node.getEngine();
        if (Languages.sameLanguage(engine.getSourceLanguage(), sourceLanguage) &&
                Languages.sameLanguage(engine.getTargetLanguage(), targetLanguage)) {
            return false;
        } else if (Languages.sameLanguage(engine.getSourceLanguage(), targetLanguage) &&
                Languages.sameLanguage(engine.getTargetLanguage(), sourceLanguage)) {
            return true;
        }
        throw new LanguagePairNotSupportedException(sourceLanguage, targetLanguage);
    }

}
