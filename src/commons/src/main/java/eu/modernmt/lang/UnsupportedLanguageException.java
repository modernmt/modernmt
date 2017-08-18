package eu.modernmt.lang;

import java.util.Locale;

/**
 * Created by davide on 27/07/17.
 */
public class UnsupportedLanguageException extends RuntimeException {

    private final LanguagePair languagePair;

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

    public UnsupportedLanguageException(Locale source, Locale target) {
        this(new LanguagePair(source, target));
    }

    public UnsupportedLanguageException(LanguagePair direction) {
        super(makeMessage(direction.source, direction.target));
        this.languagePair = direction;
    }

    public LanguagePair getLanguagePair() {
        return languagePair;
    }

}
