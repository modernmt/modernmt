package eu.modernmt.core.facade;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.core.cluster.SessionManager;
import eu.modernmt.core.facade.operations.GetFeatureWeightsOperation;
import eu.modernmt.core.facade.operations.TranslateOperation;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.decoder.moses.MosesFeature;
import eu.modernmt.engine.SystemShutdownException;
import eu.modernmt.engine.TranslationException;
import eu.modernmt.model.MultiOptionsToken;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.framework.ProcessingException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 20/04/16.
 */
public class DecoderFacade {

    // =============================
    //  Decoder Weights
    // =============================

    public Map<MosesFeature, float[]> getFeatureWeights() {
        GetFeatureWeightsOperation operation = new GetFeatureWeightsOperation();

        try {
            return ModernMT.client.submit(operation).get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException)
                throw new RuntimeException("Unexpected error while getting features weights", cause);
            else
                throw new Error("Unexpected exception: " + cause.getMessage(), cause);
        }
    }

    public void setFeatureWeights(Map<String, float[]> weights) throws IOException {
        //TODO:
//        engine.setDecoderWeights(weights);
//        sendBroadcastSignal(SIGNAL_RESET, null);
    }

    // =============================
    //  Translation session
    // =============================

    public TranslationSession openSession(List<ContextDocument> context) {
        SessionManager sessionManager = ModernMT.client.getSessionManager();
        return sessionManager.create(context);
    }

    public void closeSession(long id) {
        SessionManager sessionManager = ModernMT.client.getSessionManager();
        sessionManager.close(id);
    }

    // =============================
    //  TranslateOperation
    // =============================

    public DecoderTranslation translate(String sentence, boolean textProcessing) throws TranslationException {
        return translate(sentence, null, 0L, textProcessing, 0);
    }

    public DecoderTranslation translate(String sentence, long sessionId, boolean textProcessing) throws TranslationException {
        return translate(sentence, null, sessionId, textProcessing, 0);
    }

    public DecoderTranslation translate(String sentence, List<ContextDocument> translationContext, boolean textProcessing) throws TranslationException {
        return translate(sentence, translationContext, 0L, textProcessing, 0);
    }

    public DecoderTranslation translate(String sentence, boolean textProcessing, int nbest) throws TranslationException {
        return translate(sentence, null, 0L, textProcessing, nbest);
    }

    public DecoderTranslation translate(String sentence, long sessionId, boolean textProcessing, int nbest) throws TranslationException {
        return translate(sentence, null, sessionId, textProcessing, nbest);
    }

    public DecoderTranslation translate(String sentence, List<ContextDocument> translationContext, boolean textProcessing, int nbest) throws TranslationException {
        return translate(sentence, translationContext, 0L, textProcessing, nbest);
    }

    private DecoderTranslation translate(String text, List<ContextDocument> translationContext, long session, boolean textProcessing, int nbest) throws TranslationException {
        TranslateOperation operation;

        if (translationContext != null) {
            operation = new TranslateOperation(text, translationContext, textProcessing, nbest);
        } else if (session > 0) {
            operation = new TranslateOperation(text, session, textProcessing, nbest);
        } else {
            operation = new TranslateOperation(text, textProcessing, nbest);
        }

        DecoderTranslation rootTranslation;

        try {
            rootTranslation = ModernMT.client.submit(operation).get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof ProcessingException)
                throw new TranslationException("Problem while processing translation", cause);
            else if (cause instanceof RuntimeException)
                throw new TranslationException("Unexpected error while translating", cause);
            else
                throw new Error("Unexpected exception: " + cause.getMessage(), cause);
        }

        for (Token token : rootTranslation) {
            if (token instanceof MultiOptionsToken) {
                MultiOptionsToken mop = (MultiOptionsToken) token;

                if (!mop.hasTranslatedOptions()) {
                    String[] options = mop.getSourceOptions();
                    Translation[] translations = new Translation[options.length];

                    for (int i = 0; i < translations.length; i++) {
                        translations[i] = translate(options[i], translationContext, session, textProcessing, 0);
                    }

                    mop.setTranslatedOptions(translations);
                }
            }
        }

        return rootTranslation;
    }

}
