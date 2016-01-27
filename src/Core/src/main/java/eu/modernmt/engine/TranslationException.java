package eu.modernmt.engine;

/**
 * Created by davide on 27/01/16.
 */
public class TranslationException extends Exception {

    public TranslationException() {
    }

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TranslationException(Throwable cause) {
        super(cause);
    }

}
