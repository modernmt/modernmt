package eu.modernmt.core.facade.exceptions;

/**
 * Created by davide on 19/05/16.
 */
public abstract class ValidationException extends Exception {

    public ValidationException(Throwable cause) {
        super(cause);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValidationException(String message) {
        super(message);
    }

}
