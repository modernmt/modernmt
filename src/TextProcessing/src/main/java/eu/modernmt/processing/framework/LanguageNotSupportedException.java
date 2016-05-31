package eu.modernmt.processing.framework;

import java.util.Locale;

/**
 * Created by davide on 31/05/16.
 */
public class LanguageNotSupportedException extends ProcessingException {

    private static String toString(Locale language) {
        return language == null ? "[null]" : language.toLanguageTag();
    }

    public LanguageNotSupportedException(Locale language) {
        super("Language not supported: " + toString(language));
    }

    public LanguageNotSupportedException(Locale source, Locale target) {
        super("Language pair not supported: " + toString(source) + " > " + toString(target));
    }
}
