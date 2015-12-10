package eu.modernmt.engine;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.TranslationSession;

import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 09/12/15.
 */
public class DistributedTranslationSession extends TranslationSession {

    private static long sequence = 1L;

    private static synchronized long nextSeq() {
        return sequence++;
    }

    public DistributedTranslationSession(List<ContextDocument> translationContext) {
        super(nextSeq(), translationContext);
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }
}
