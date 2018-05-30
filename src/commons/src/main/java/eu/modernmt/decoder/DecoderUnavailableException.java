package eu.modernmt.decoder;

/**
 * Created by davide on 22/05/17.
 */
public class DecoderUnavailableException extends DecoderException {

    public DecoderUnavailableException(String message) {
        super(message);
    }

    public DecoderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecoderUnavailableException(Throwable cause) {
        super(cause);
    }

}
