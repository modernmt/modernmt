package eu.modernmt.core.cluster.error;

/**
 * Created by davide on 20/04/16.
 */
public class ClusterException extends Exception {

    public ClusterException() {
    }

    public ClusterException(String message) {
        super(message);
    }

    public ClusterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterException(Throwable cause) {
        super(cause);
    }

}
