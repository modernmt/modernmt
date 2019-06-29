package eu.modernmt.lang;

/**
 * Created by davide on 27/07/17.
 */
public class UnsupportedLanguageException extends RuntimeException {

    private final LanguageDirection languageDirection;

    private static String toString(Language2 language) {
        return language == null ? "[null]" : language.toLanguageTag();
    }

    private static String makeMessage(Language2 source, Language2 target) {
        if (source != null && target != null)
            return "Language2 pair not supported: " + toString(source) + " > " + toString(target);
        else
            return "Language2 not supported: " + toString(source == null ? target : source);
    }

    public UnsupportedLanguageException(Language2 language) {
        this(language, null);
    }

    public UnsupportedLanguageException(Language2 source, Language2 target) {
        this(new LanguageDirection(source, target));
    }

    public UnsupportedLanguageException(LanguageDirection direction) {
        super(makeMessage(direction.source, direction.target));
        this.languageDirection = direction;
    }

    public LanguageDirection getLanguageDirection() {
        return languageDirection;
    }

}
