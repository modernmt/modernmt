package eu.modernmt.cluster.error;

/**
 * Created by davide on 26/01/16.
 */
public class SystemShutdownException extends RuntimeException {

    public SystemShutdownException() {
    }

    public SystemShutdownException(String message) {
        super(message);
    }

    public SystemShutdownException(String message, Throwable cause) {
        super(message, cause);
    }

    public SystemShutdownException(Throwable cause) {
        super(cause);
    }
}
