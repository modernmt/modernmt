package eu.modernmt.processing.framework;

/**
 * Created by davide on 26/01/16.
 */
public class ProcessingException extends Exception {

    public ProcessingException() {
    }

    public ProcessingException(String message) {
        super(message);
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessingException(Throwable cause) {
        super(cause);
    }
    
}
