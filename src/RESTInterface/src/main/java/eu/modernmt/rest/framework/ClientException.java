package eu.modernmt.rest.framework;

/**
 * Created by davide on 15/12/15.
 */
public class ClientException extends Exception {

    private int code;

    public ClientException() {
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientException(Throwable cause) {
        super(cause);
    }

    public ClientException(int code) {
        this.code = code;
    }

    public ClientException(String message, int code) {
        super(message);
        this.code = code;
    }

    public ClientException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public ClientException(Throwable cause, int code) {
        super(cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
