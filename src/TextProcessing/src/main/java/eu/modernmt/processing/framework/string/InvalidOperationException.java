package eu.modernmt.processing.framework.string;

import eu.modernmt.processing.framework.ProcessingException;

/**
 * Created by lucamastrostefano on 30/03/16.
 */
public class InvalidOperationException extends ProcessingException {

    public InvalidOperationException(XMLEditableString.Operation operation, TokenHook tokenHook,
                                     String message) {
        super(message + " " + operation.toString() + " " + tokenHook.toString());
    }

    public InvalidOperationException(int startIndex, int lastEditedIndex) {
        super("Not monotonic operations: operation startIndex must be >= than the last" +
                " edited index (startIndex: " + startIndex + ", last edited index: " + lastEditedIndex);
    }

    public InvalidOperationException(XMLEditableString.Operation operation, TokenHook tokenHook) {
        this(operation, tokenHook, "Overlapping operations:");
    }
    
}
