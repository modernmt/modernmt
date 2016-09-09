package eu.modernmt.decoder;

import eu.modernmt.context.ContextScore;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Created by davide on 02/12/15.
 */
public class TranslationSession implements Serializable, Closeable {

    protected final long id;
    protected final List<ContextScore> translationContext;

    public TranslationSession(long id, List<ContextScore> translationContext) {
        this.id = id;
        this.translationContext = translationContext;
    }

    public long getId() {
        return id;
    }

    public List<ContextScore> getTranslationContext() {
        return translationContext;
    }

    @Override
    public void close() throws IOException {
        // Default does nothing
    }
}
