package eu.modernmt.datastream;

/**
 * Created by davide on 06/09/16.
 */
public class DataStreamException extends Exception {

    public DataStreamException(String message) {
        super(message);
    }

    public DataStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataStreamException(Throwable cause) {
        super(cause);
    }

}
