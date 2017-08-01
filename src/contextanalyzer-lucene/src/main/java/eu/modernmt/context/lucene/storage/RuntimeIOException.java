package eu.modernmt.context.lucene.storage;

import java.io.IOException;

/**
 * Created by davide on 01/08/17.
 */
class RuntimeIOException extends RuntimeException {

    public final IOException cause;

    RuntimeIOException(IOException cause) {
        super(cause);
        this.cause = cause;
    }

}
