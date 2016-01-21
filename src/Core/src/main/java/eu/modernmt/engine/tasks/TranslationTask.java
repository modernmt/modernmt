package eu.modernmt.engine.tasks;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.*;
import eu.modernmt.engine.MMTWorker;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.tokenizer.DetokenizerPool;
import eu.modernmt.tokenizer.TokenizerPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 09/12/15.
 */
public class TranslationTask extends DistributedCallable<Translation> {

    private Sentence source;
    private List<ContextDocument> translationContext;
    private Long session;
    private boolean processing;
    private int nbest;

    public TranslationTask(Sentence source, boolean processing, int nbest) {
        this.source = source;
        this.processing = processing;
        this.nbest = nbest;
    }

    public TranslationTask(Sentence source, List<ContextDocument> translationContext, boolean processing, int nbest) {
        this.source = source;
        this.translationContext = translationContext;
        this.processing = processing;
        this.nbest = nbest;
    }

    public TranslationTask(Sentence source, TranslationSession session, boolean processing, int nbest) {
        this.source = source;
        this.session = session.getId();
        this.translationContext = session.getTranslationContext();
        this.processing = processing;
        this.nbest = nbest;
    }

    @Override
    public MMTWorker getWorker() {
        return (MMTWorker) super.getWorker();
    }

    @Override
    public Translation call() throws IOException {
        MMTWorker worker = getWorker();
        Decoder decoder = worker.getDecoder();

        Sentence tokenizedSource;
        Translation translation;

        if (processing) {
            TokenizerPool tokenizer = worker.getTokenizer();
            tokenizedSource = new Sentence(tokenizer.tokenize(source.toString()));
        } else {
            tokenizedSource = source;
        }

        if (session != null) {
            TranslationSession session = decoder.getSession(this.session);

            if (session == null)
                if (translationContext == null)
                    throw new IllegalArgumentException("Session id is new, but no context has been provided.");
                else
                    session = decoder.openSession(this.session, translationContext);

            translation = nbest > 0 ? decoder.translate(tokenizedSource, session, nbest) : decoder.translate(tokenizedSource, session);
        } else if (translationContext != null) {
            translation = nbest > 0 ? decoder.translate(tokenizedSource, translationContext, nbest) : decoder.translate(tokenizedSource, translationContext);
        } else {
            translation = nbest > 0 ? decoder.translate(tokenizedSource, nbest) : decoder.translate(tokenizedSource);
        }

        if (processing) {
            DetokenizerPool detokenizer = worker.getDetokenizer();

            List<TranslationHypothesis> nbest = translation.getNbest();
            translation = new Translation(detokenizer.detokenize(translation.getTokens()), source);

            if (nbest != null) {
                List<TranslationHypothesis> detokNBest = new ArrayList<>(nbest.size());
                List<String[]> strings = new ArrayList<>(nbest.size());

                for (TranslationHypothesis hyp : nbest)
                    strings.add(hyp.getTokens());


                List<String> detokStrings = detokenizer.detokenize(strings);

                for (int i = 0; i < detokStrings.size(); i++) {
                    TranslationHypothesis original = nbest.get(i);
                    String newtext = detokStrings.get(i);

                    TranslationHypothesis hyp = new TranslationHypothesis(newtext, original.getTotalScore(), original.getScores());
                    detokNBest.add(hyp);
                }

                translation.setNbest(detokNBest);
            }
        }

        return translation;
    }

}
