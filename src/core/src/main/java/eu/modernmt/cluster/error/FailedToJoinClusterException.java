package eu.modernmt.cluster.error;

/**
 * Created by davide on 20/04/16.
 */
public class FailedToJoinClusterException extends ClusterException {

    private static String getMessage(String address) {
        StringBuilder message = new StringBuilder("Could not join cluster.");
        if (address != null) {
            message.append(" Failed to connect to: ");
            message.append(address);
        }

        return message.toString();
    }

    public FailedToJoinClusterException(String address) {
        super(getMessage(address));
    }
}
