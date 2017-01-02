package eu.modernmt.data;

/**
 * Created by davide on 06/09/16.
 */
public class DataManagerException extends Exception {

    public DataManagerException(String message) {
        super(message);
    }

    public DataManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataManagerException(Throwable cause) {
        super(cause);
    }

}
