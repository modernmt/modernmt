package eu.modernmt.decoder.neural;

import eu.modernmt.decoder.DecoderException;

/**
 * Created by davide on 22/05/17.
 */
public class NeuralDecoderException extends DecoderException {

    public static NeuralDecoderException fromPythonError(String type, String message) {
        if (message == null)
            return new NeuralDecoderException(type);
        else
            return new NeuralDecoderException(type + " - " + message);
    }

    public NeuralDecoderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NeuralDecoderException(String message) {
        super(message);
    }
}
