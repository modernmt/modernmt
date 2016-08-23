package eu.modernmt.processing.framework;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 26/01/16.
 */
public abstract class TextProcessor<P, R> {

    public static final String KEY_TOKENIZE_TEXT = "TextProcessor.TOKENIZE_TEXT"; // Default true
    public static final String KEY_VOCABULARY = "TextProcessor.VOCABULARY"; // Default null

    public TextProcessor(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
    }

    public abstract R call(P param, Map<String, Object> metadata) throws ProcessingException;

}
