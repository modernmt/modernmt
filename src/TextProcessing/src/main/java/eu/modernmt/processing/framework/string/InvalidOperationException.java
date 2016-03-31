package eu.modernmt.processing.framework.string;

/**
 * Created by lucamastrostefano on 30/03/16.
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(XMLEditableString.Operation operation, XMLEditableString.TokenHook tokenHook,
                                     String message) {
        super(message + " " + operation.toString() + " " + tokenHook.toString());
    }

    public InvalidOperationException(int startIndex, int lastEditedIndex) {
        super("Not monotonic operations: operation startIndex must be >= than the last" +
                " edited index (startIndex: " + startIndex + ", last edited index: " + lastEditedIndex);
    }

    public InvalidOperationException(XMLEditableString.Operation operation, XMLEditableString.TokenHook tokenHook) {
        this(operation, tokenHook, "Overlapping operations:");
    }
}
