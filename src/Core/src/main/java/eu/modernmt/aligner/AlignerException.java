package eu.modernmt.aligner;

/**
 * Created by davide on 22/04/16.
 */
public class AlignerException extends Exception {

    public AlignerException(String message) {
        super(message);
    }

    public AlignerException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlignerException(Throwable cause) {
        super(cause);
    }

}
