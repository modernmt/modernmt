package eu.modernmt.data;

/**
 * Created by davide on 06/09/16.
 */
public class HostUnreachableException extends DataManagerException {

    public HostUnreachableException(String host) {
        super("Host unreachable: " + host);
    }

}
