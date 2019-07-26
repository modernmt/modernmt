package eu.modernmt.cluster.error;

/**
 * Created by davide on 26/01/16.
 */
public class SystemShutdownException extends RuntimeException {

    public SystemShutdownException(Throwable cause) {
        super(cause);
    }
    
}
