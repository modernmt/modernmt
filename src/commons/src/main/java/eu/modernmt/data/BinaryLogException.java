package eu.modernmt.data;

/**
 * Created by davide on 06/09/16.
 */
public class BinaryLogException extends Exception {

    public BinaryLogException(String message) {
        super(message);
    }

    public BinaryLogException(String message, Throwable cause) {
        super(message, cause);
    }

    public BinaryLogException(Throwable cause) {
        super(cause);
    }

}
