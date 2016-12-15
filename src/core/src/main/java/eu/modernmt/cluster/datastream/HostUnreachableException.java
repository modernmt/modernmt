package eu.modernmt.cluster.datastream;

/**
 * Created by davide on 06/09/16.
 */
public class HostUnreachableException extends DataStreamException {

    public HostUnreachableException(String host) {
        super("Host unreachable: " + host);
    }

}
