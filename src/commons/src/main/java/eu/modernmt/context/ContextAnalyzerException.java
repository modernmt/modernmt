package eu.modernmt.context;

/**
 * Created by davide on 26/01/16.
 */
public class ContextAnalyzerException extends Exception {

    public ContextAnalyzerException(String message) {
        super(message);
    }

    public ContextAnalyzerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContextAnalyzerException(Throwable cause) {
        super(cause);
    }

}
