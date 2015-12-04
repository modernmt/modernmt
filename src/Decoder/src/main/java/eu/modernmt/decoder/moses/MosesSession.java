package eu.modernmt.decoder.moses;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.TranslationSession;

import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 01/12/15.
 */
public class MosesSession extends TranslationSession {

    private MosesDecoder moses;

    public MosesSession(long id, List<ContextDocument> translationContext, MosesDecoder moses) {
        super(id, translationContext);
        this.moses = moses;
    }

    @Override
    public void close() throws IOException {
        this.moses.closeSession(this.id);
    }

}
