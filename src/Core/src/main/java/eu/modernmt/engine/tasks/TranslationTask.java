package eu.modernmt.engine.tasks;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.engine.SlaveNode;
import eu.modernmt.model.Sentence;
import eu.modernmt.network.cluster.DistributedCallable;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.framework.ProcessingException;

import java.util.List;

/**
 * Created by davide on 09/12/15.
 */
public class TranslationTask extends DistributedCallable<DecoderTranslation> {

    private String text;
    private List<ContextDocument> translationContext;
    private Long session;
    private boolean processing;
    private int nbest;

    public TranslationTask(String text, boolean processing, int nbest) {
        this.text = text;
        this.processing = processing;
        this.nbest = nbest;
    }

    public TranslationTask(String text, List<ContextDocument> translationContext, boolean processing, int nbest) {
        this.text = text;
        this.translationContext = translationContext;
        this.processing = processing;
        this.nbest = nbest;
    }

    public TranslationTask(String text, TranslationSession session, boolean processing, int nbest) {
        this.text = text;
        this.session = session.getId();
        this.translationContext = session.getTranslationContext();
        this.processing = processing;
        this.nbest = nbest;
    }

    @Override
    public SlaveNode getWorker() {
        return (SlaveNode) super.getWorker();
    }

    @Override
    public DecoderTranslation call() throws ProcessingException {
        SlaveNode worker = getWorker();
        Decoder decoder = worker.getDecoder();

        Sentence sentence = worker.getPreprocessor().process(text, processing);

        DecoderTranslation translation;
        if (session != null) {
            TranslationSession session = decoder.getSession(this.session);

            if (session == null)
                if (translationContext == null)
                    throw new IllegalArgumentException("Session id is new, but no context has been provided.");
                else
                    session = decoder.openSession(this.session, translationContext);

            translation = nbest > 0 ? decoder.translate(sentence, session, nbest) : decoder.translate(sentence, session);
        } else if (translationContext != null) {
            translation = nbest > 0 ? decoder.translate(sentence, translationContext, nbest) : decoder.translate(sentence, translationContext);
        } else {
            translation = nbest > 0 ? decoder.translate(sentence, nbest) : decoder.translate(sentence);
        }

        Postprocessor postprocessor = worker.getPostprocessor();
        postprocessor.process(translation, processing);
        if (translation.hasNbest())
            postprocessor.process(translation.getNbest(), processing);

        return translation;
    }

}
