package eu.modernmt.processing;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;

import java.util.Map;

/**
 * Created by davide on 26/01/16.
 */
public abstract class TextProcessor<P, R> {

    public TextProcessor(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
    }

    public abstract R call(P param, Map<String, Object> metadata) throws ProcessingException;

}
