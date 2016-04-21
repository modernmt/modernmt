package eu.modernmt.engine;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.TranslationSession;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 09/12/15.
 */
public class DistributedTranslationSession extends TranslationSession implements Closeable {

    private static long sequence = 1L;

    private static synchronized long nextSeq() {
        return sequence++;
    }

    private MasterNode server;

    public DistributedTranslationSession(List<ContextDocument> translationContext, MasterNode server) {
        super(nextSeq(), translationContext);
        this.server = server;
    }

    @Override
    public void close() throws IOException {
        // TODO: should broadcast to workers to close their Moses Sessions - server.sendBroadcastSignal(CLOSE_SESSION, id);
    }
}
