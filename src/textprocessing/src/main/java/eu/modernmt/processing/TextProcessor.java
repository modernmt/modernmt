package eu.modernmt.processing;

import eu.modernmt.model.UnsupportedLanguageException;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 26/01/16.
 */
public abstract class TextProcessor<P, R> {

    public TextProcessor(Locale sourceLanguage, Locale targetLanguage) throws UnsupportedLanguageException {
    }

    public abstract R call(P param, Map<String, Object> metadata) throws ProcessingException;

}
