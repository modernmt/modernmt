package eu.modernmt.decoder.moses;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.TranslationSession;

import java.util.List;

/**
 * Created by davide on 01/12/15.
 */
public class MosesSession extends TranslationSession {

    private MosesDecoder moses;
    private long internalId;

    public MosesSession(long id, List<ContextDocument> translationContext, MosesDecoder moses, long internalId) {
        super(id, translationContext);
        this.moses = moses;
        this.internalId = internalId;
    }

    @Override
    public void close() {
        this.moses.closeSession(this);
    }

    public long getInternalId() {
        return internalId;
    }

}
