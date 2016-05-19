package eu.modernmt.core.facade.exceptions;

/**
 * Created by davide on 19/05/16.
 */
public abstract class InternalErrorException extends Exception {

    public InternalErrorException(String message) {
        super(message);
    }

    public InternalErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalErrorException(Throwable cause) {
        super(cause);
    }
    
}
