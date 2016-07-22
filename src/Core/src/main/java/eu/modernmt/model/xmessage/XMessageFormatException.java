package eu.modernmt.model.xmessage;

/**
 * Created by davide on 07/04/16.
 */
public class XMessageFormatException extends Exception {

    public XMessageFormatException() {
    }

    public XMessageFormatException(String message) {
        super(message);
    }

    public XMessageFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public XMessageFormatException(Throwable cause) {
        super(cause);
    }
}
