package eu.modernmt.decoder.neural;

/**
 * Created by davide on 22/05/17.
 */
public class NeuralDecoderRejectedExecutionException extends NeuralDecoderException {

    public NeuralDecoderRejectedExecutionException() {
        super("Execution rejected: decoder has been closed");
    }

}
