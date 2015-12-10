package eu.modernmt.decoder;

import eu.modernmt.context.ContextDocument;

import java.io.Closeable;
import java.io.Serializable;
import java.util.List;

/**
 * Created by davide on 02/12/15.
 */
public abstract class TranslationSession implements Closeable, Serializable {

    protected final long id;
    protected final List<ContextDocument> translationContext;

    protected TranslationSession(long id, List<ContextDocument> translationContext) {
        this.id = id;
        this.translationContext = translationContext;
    }

    public long getId() {
        return id;
    }

    public List<ContextDocument> getTranslationContext() {
        return translationContext;
    }

}
