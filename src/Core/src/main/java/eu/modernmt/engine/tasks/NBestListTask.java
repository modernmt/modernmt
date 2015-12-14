package eu.modernmt.engine.tasks;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.Sentence;
import eu.modernmt.decoder.TranslationHypothesis;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.engine.MMTWorker;
import eu.modernmt.engine.TranslationEngine;
import eu.modernmt.network.cluster.DistributedCallable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 09/12/15.
 */
public class NBestListTask extends DistributedCallable<ArrayList<TranslationHypothesis>> {

    private int nbest;
    private Sentence sentence;
    private List<ContextDocument> translationContext;
    private Long session;

    public NBestListTask(Sentence sentence, int nbest) {
        this.sentence = sentence;
        this.nbest = nbest;
    }

    public NBestListTask(Sentence sentence, List<ContextDocument> translationContext, int nbest) {
        this.sentence = sentence;
        this.translationContext = translationContext;
        this.nbest = nbest;
    }

    public NBestListTask(Sentence sentence, TranslationSession session, int nbest) {
        this.sentence = sentence;
        this.session = session.getId();
        this.translationContext = session.getTranslationContext();
        this.nbest = nbest;
    }

    @Override
    public MMTWorker getWorker() {
        return (MMTWorker) super.getWorker();
    }

    @Override
    public ArrayList<TranslationHypothesis> call() throws IOException {
        TranslationEngine engine = getWorker().getEngine();
        Decoder decoder = engine.getDecoder();

        List<TranslationHypothesis> result;

        if (session != null) {
            TranslationSession session = decoder.getSession(this.session);

            if (session == null)
                if (translationContext == null)
                    throw new IllegalArgumentException("Session id is new, but no context has been provided.");
                else
                    session = decoder.openSession(this.session, translationContext);

            result = decoder.translate(sentence, session, nbest);
        } else if (translationContext != null) {
            result = decoder.translate(sentence, translationContext, nbest);
        } else {
            result = decoder.translate(sentence, nbest);
        }

        if (result instanceof ArrayList) {
            return (ArrayList<TranslationHypothesis>) result;
        } else {
            return new ArrayList<>(result);
        }
    }

}
