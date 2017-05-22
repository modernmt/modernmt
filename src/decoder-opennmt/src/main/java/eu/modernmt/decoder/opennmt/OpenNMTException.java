package eu.modernmt.decoder.opennmt;

import eu.modernmt.decoder.DecoderException;

/**
 * Created by davide on 22/05/17.
 */
public class OpenNMTException extends DecoderException {

    public static OpenNMTException fromPythonError(String type, String message) {
        if (message == null)
            return new OpenNMTException(type);
        else
            return new OpenNMTException(type + " - " + message);
    }

    public OpenNMTException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenNMTException(String message) {
        super(message);
    }
}
