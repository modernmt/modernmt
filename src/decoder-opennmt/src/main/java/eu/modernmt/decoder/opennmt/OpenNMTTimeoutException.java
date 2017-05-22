package eu.modernmt.decoder.opennmt;

/**
 * Created by davide on 22/05/17.
 */
public class OpenNMTTimeoutException extends OpenNMTException {

    public OpenNMTTimeoutException() {
        super("Translation request timeout occurred");
    }

}
