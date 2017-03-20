package eu.modernmt.persistence.cassandra;

import eu.modernmt.persistence.PersistenceException;

/**
 * Created by andrea on 09/03/17.
 */
public class KeyspaceNotFoundException extends PersistenceException {


    public KeyspaceNotFoundException(String message) {
        super(message);
    }

    public KeyspaceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyspaceNotFoundException(Throwable cause) {
        super(cause);
    }
}
