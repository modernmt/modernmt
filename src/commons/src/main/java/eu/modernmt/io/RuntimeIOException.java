package eu.modernmt.io;

import java.io.IOException;

/**
 * Created by davide on 11/12/17.
 */
public class RuntimeIOException extends RuntimeException {

    public RuntimeIOException(IOException cause) {
        super(cause);
    }

}
