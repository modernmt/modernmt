package eu.modernmt;

public class RuntimeErrorException extends RuntimeException {

    private static String getMessage(String reason) {
        if (reason == null)
            return "Unexpected error";
        else
            return "Unexpected error: " + reason;
    }

    public RuntimeErrorException(String reason, Throwable cause) {
        super(getMessage(reason), cause);
    }

    public RuntimeErrorException(Throwable cause) {
        this(null, cause);
    }
}
