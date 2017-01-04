package eu.modernmt.engine;

/**
 * Created by davide on 19/04/16.
 */
public class BootstrapException extends Exception {

    public BootstrapException(Throwable cause) {
        super(cause);
    }

    public BootstrapException(String message) {
        super(message);
    }

    public BootstrapException(String message, Throwable cause) {
        super(message, cause);
    }

    protected BootstrapException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
