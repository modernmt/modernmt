package eu.modernmt.facade.operations;

import eu.modernmt.engine.Engine;
import eu.modernmt.cluster.SessionManager;
import eu.modernmt.context.ContextScore;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;

import java.util.List;

/**
 * Created by davide on 21/04/16.
 */
public class TranslateOperation extends Operation<DecoderTranslation> {

    private String text;
    private List<ContextScore> translationContext;
    private Long session;
    private int nbest;

    public TranslateOperation(String text, int nbest) {
        this.text = text;
        this.nbest = nbest;
    }

    public TranslateOperation(String text, List<ContextScore> translationContext, int nbest) {
        this.text = text;
        this.translationContext = translationContext;
        this.nbest = nbest;
    }

    public TranslateOperation(String text, long session, int nbest) {
        this.text = text;
        this.session = session;
        this.translationContext = null;
        this.nbest = nbest;
    }

    @Override
    public DecoderTranslation call() throws ProcessingException {
        Engine engine = getEngine();
        Decoder decoder = engine.getDecoder();
        Preprocessor preprocessor = engine.getSourcePreprocessor();
        Postprocessor postprocessor = engine.getPostprocessor();

        Sentence sentence = preprocessor.process(text);

        DecoderTranslation translation;
        if (session != null) {
            SessionManager sessionManager = getLocalNode().getSessionManager();
            TranslationSession session = sessionManager.get(this.session);

            if (session == null)
                throw new IllegalArgumentException("Session not found: " + this.session);

            translation = nbest > 0 ? decoder.translate(sentence, session, nbest) : decoder.translate(sentence, session);
        } else if (translationContext != null) {
            translation = nbest > 0 ? decoder.translate(sentence, translationContext, nbest) : decoder.translate(sentence, translationContext);
        } else {
            translation = nbest > 0 ? decoder.translate(sentence, nbest) : decoder.translate(sentence);
        }

        postprocessor.process(translation);
        if (translation.hasNbest())
            postprocessor.process(translation.getNbest());

        return translation;
    }

}
