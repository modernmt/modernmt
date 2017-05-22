package eu.modernmt.decoder;

/**
 * Created by davide on 22/05/17.
 */
public class DecoderException extends Exception {

    public DecoderException(String message) {
        super(message);
    }

    public DecoderException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecoderException(Throwable cause) {
        super(cause);
    }

}
