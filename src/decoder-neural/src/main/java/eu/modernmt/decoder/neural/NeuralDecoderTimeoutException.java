package eu.modernmt.decoder.neural;

/**
 * Created by davide on 22/05/17.
 */
public class NeuralDecoderTimeoutException extends NeuralDecoderException {

    public NeuralDecoderTimeoutException() {
        super("Translation request timeout occurred");
    }

}
