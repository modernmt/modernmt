package eu.modernmt.core.facade.exceptions;

/**
 * Created by davide on 19/05/16.
 */
public abstract class AuthenticationException extends Exception {

    public AuthenticationException(Throwable cause) {
        super(cause);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationException(String message) {
        super(message);
    }

}
