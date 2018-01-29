package eu.modernmt.facade.exceptions;

public class TestFailedException extends Exception {

    public TestFailedException(Throwable cause) {
        super("Test failed: (" + cause.getClass().getSimpleName() + ") " + cause.getMessage(), cause);
    }

    public TestFailedException(String message, Throwable cause) {
        super(message + ": (" + cause.getClass().getSimpleName() + ") " + cause.getMessage(), cause);
    }
}
