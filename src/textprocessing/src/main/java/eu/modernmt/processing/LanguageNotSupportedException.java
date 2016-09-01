package eu.modernmt.processing;

import java.util.Locale;

/**
 * Created by davide on 31/05/16.
 */
public class LanguageNotSupportedException extends ProcessingException {

    private static String toString(Locale language) {
        return language == null ? "[null]" : language.toLanguageTag();
    }

    private static String makeMessage(Locale source, Locale target) {
        if (source != null && target != null)
            return "Language pair not supported: " + toString(source) + " > " + toString(target);
        else
            return "Language not supported: " + toString(source == null ? target : source);
    }

    public LanguageNotSupportedException(Locale language) {
        this(language, null);
    }

    public LanguageNotSupportedException(Locale source, Locale target) {
        super(makeMessage(source, target));
    }
}
