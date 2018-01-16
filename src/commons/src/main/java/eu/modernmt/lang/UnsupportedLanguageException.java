package eu.modernmt.lang;

/**
 * Created by davide on 27/07/17.
 */
public class UnsupportedLanguageException extends RuntimeException {

    private final LanguagePair languagePair;

    private static String toString(Language language) {
        return language == null ? "[null]" : language.toLanguageTag();
    }

    private static String makeMessage(Language source, Language target) {
        if (source != null && target != null)
            return "Language pair not supported: " + toString(source) + " > " + toString(target);
        else
            return "Language not supported: " + toString(source == null ? target : source);
    }

    public UnsupportedLanguageException(Language language) {
        this(language, null);
    }

    public UnsupportedLanguageException(Language source, Language target) {
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
