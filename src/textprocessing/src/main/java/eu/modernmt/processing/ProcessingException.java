package eu.modernmt.processing;

/**
 * Created by davide on 26/01/16.
 */
public class ProcessingException extends Exception {

    public ProcessingException(Object input, TextProcessor<?, ?> processor, RuntimeException cause) {
        super('(' + processor.getClass().getSimpleName() + ") Failed to process input \"" + input + "\"", cause);
    }

}
