package eu.modernmt.decoder;

/**
 * Created by davide on 22/04/16.
 */
public class TranslationException extends Exception {

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
