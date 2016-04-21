package eu.modernmt.core.cluster.error;

/**
 * Created by davide on 19/04/16.
 */
public class BootstrapException extends ClusterException {

    public BootstrapException(String message) {
        super(message);
    }

    public BootstrapException(String message, Throwable cause) {
        super(message, cause);
    }

    public BootstrapException(Throwable cause) {
        super(cause);
    }

}
