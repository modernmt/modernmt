package eu.modernmt.decoder;

import eu.modernmt.model.ContextVector;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by davide on 02/12/15.
 */
public class TranslationSession implements Serializable, Closeable {

    protected final long id;
    protected final ContextVector contextVector;

    public TranslationSession(long id, ContextVector contextVector) {
        this.id = id;
        this.contextVector = contextVector;
    }

    public long getId() {
        return id;
    }

    public ContextVector getContextVector() {
        return contextVector;
    }

    @Override
    public void close() throws IOException {
        // Default does nothing
    }
}
