package eu.modernmt.data;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by davide on 06/09/16.
 */
public class HostUnreachableException extends BinaryLogException {

    public HostUnreachableException(String[] hosts, int port) {
        super("Host unreachable (port " + port + "): " + StringUtils.join(hosts, ','));
    }

}
