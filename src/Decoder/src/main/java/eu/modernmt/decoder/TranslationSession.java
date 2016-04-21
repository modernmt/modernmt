package eu.modernmt.decoder;

import eu.modernmt.context.ContextDocument;

import java.io.Serializable;
import java.util.List;

/**
 * Created by davide on 02/12/15.
 */
public class TranslationSession implements Serializable {

    protected final long id;
    protected final List<ContextDocument> translationContext;

    public TranslationSession(long id, List<ContextDocument> translationContext) {
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
