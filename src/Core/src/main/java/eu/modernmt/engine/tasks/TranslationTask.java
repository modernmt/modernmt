package eu.modernmt.engine.tasks;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.Sentence;
import eu.modernmt.decoder.Translation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.engine.MMTWorker;
import eu.modernmt.engine.TranslationEngine;
import eu.modernmt.network.cluster.DistributedCallable;

import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 09/12/15.
 */
public class TranslationTask extends DistributedCallable<Translation> {

    private Sentence sentence;
    private List<ContextDocument> translationContext;
    private Long session;

    public TranslationTask(Sentence sentence) {
        this.sentence = sentence;
    }

    public TranslationTask(Sentence sentence, List<ContextDocument> translationContext) {
        this.sentence = sentence;
        this.translationContext = translationContext;
    }

    public TranslationTask(Sentence sentence, TranslationSession session) {
        this.sentence = sentence;
        this.session = session.getId();
        this.translationContext = session.getTranslationContext();
    }

    @Override
    public MMTWorker getWorker() {
        return (MMTWorker) super.getWorker();
    }

    @Override
    public Translation call() throws IOException {
        TranslationEngine engine = getWorker().getEngine();
        Decoder decoder = engine.getDecoder();

        if (session != null) {
            TranslationSession session = decoder.getSession(this.session);

            if (session == null)
                if (translationContext == null)
                    throw new IllegalArgumentException("Session id is new, but no context has been provided.");
                else
                    session = decoder.openSession(this.session, translationContext);

            return decoder.translate(sentence, session);
        } else if (translationContext != null) {
            return decoder.translate(sentence, translationContext);
        } else {
            return decoder.translate(sentence);
        }
    }

}
