package eu.modernmt.core.cluster.error;

/**
 * Created by davide on 20/04/16.
 */
public class FailedToJoinClusterException extends ClusterException {

    public FailedToJoinClusterException(String address) {
        super("Could not join cluster. Failed to connect to: " + address);
    }
}
