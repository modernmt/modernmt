package eu.modernmt.model;

import java.util.Locale;

/**
 * Created by davide on 27/07/17.
 */
public class UnsupportedLanguageException extends RuntimeException {

    private static String toString(Locale language) {
        return language == null ? "[null]" : language.toLanguageTag();
    }

    private static String makeMessage(Locale source, Locale target) {
        if (source != null && target != null)
            return "Language pair not supported: " + toString(source) + " > " + toString(target);
        else
            return "Language not supported: " + toString(source == null ? target : source);
    }

    public UnsupportedLanguageException(Locale language) {
        this(language, null);
    }

    public UnsupportedLanguageException(LanguagePair direction) {
        this(direction.source, direction.target);
    }

    public UnsupportedLanguageException(Locale source, Locale target) {
        super(makeMessage(source, target));
    }

}
