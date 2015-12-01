package eu.modernmt.decoder.moses;

import eu.modernmt.decoder.DecoderSession;
import eu.modernmt.model.context.TranslationContext;

import java.io.IOException;

/**
 * Created by davide on 01/12/15.
 */
public class MosesSession extends DecoderSession {

    private MosesDecoder moses;

    public MosesSession(MosesDecoder moses, long id, TranslationContext translationContext) {
        super(id, translationContext);
        this.moses = moses;
    }

    public MosesSession(MosesDecoder moses, long id, TranslationContext translationContext, long lifetime) {
        super(id, translationContext, lifetime);
        this.moses = moses;
    }

    @Override
    public void close() throws IOException {
        this.moses.closeSession(this.id);
    }

}
