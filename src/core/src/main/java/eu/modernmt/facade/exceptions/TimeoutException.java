package eu.modernmt.facade.exceptions;

import eu.modernmt.decoder.DecoderException;

public class TimeoutException extends DecoderException {

    public TimeoutException() {
        super("Translation request timed out");
    }

}
