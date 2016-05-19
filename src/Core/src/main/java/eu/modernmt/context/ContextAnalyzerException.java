package eu.modernmt.context;

import eu.modernmt.core.facade.exceptions.InternalErrorException;

/**
 * Created by davide on 26/01/16.
 */
public class ContextAnalyzerException extends InternalErrorException {

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
