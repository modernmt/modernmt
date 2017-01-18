package eu.modernmt.facade;

import eu.modernmt.cluster.SessionManager;
import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.decoder.*;
import eu.modernmt.facade.operations.TranslateOperation;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.MultiOptionsToken;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.ProcessingException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 20/04/16.
 */
public class DecoderFacade {

    // =============================
    //  Decoder Weights
    // =============================

    public Map<DecoderFeature, float[]> getFeatureWeights() {
        // Invoke on local decoder instance because it's just a matter of
        // properties reading and not a real computation
        Decoder decoder = ModernMT.getNode().getEngine().getDecoder();

        HashMap<DecoderFeature, float[]> result = new HashMap<>();
        for (DecoderFeature feature : decoder.getFeatures()) {
            float[] weights = feature.isTunable() ? decoder.getFeatureWeights(feature) : null;
            result.put(feature, weights);
        }

        return result;
    }

    public void setFeatureWeights(Map<String, float[]> weights) {
        ModernMT.getNode().notifyDecoderWeightsChanged(weights);
    }

    // =============================
    //  Translation session
    // =============================

    public TranslationSession openSession(ContextVector context) {
        SessionManager sessionManager = ModernMT.getNode().getSessionManager();
        return sessionManager.create(context);
    }

    public TranslationSession getSession(long id) {
        SessionManager sessionManager = ModernMT.getNode().getSessionManager();
        return sessionManager.get(id);
    }

    // =============================
    //  TranslateOperation
    // =============================

    public DecoderTranslation translate(String sentence) throws TranslationException {
        return translate(sentence, null, 0L, 0);
    }

    public DecoderTranslation translate(String sentence, long sessionId) throws TranslationException {
        return translate(sentence, null, sessionId, 0);
    }

    public DecoderTranslation translate(String sentence, ContextVector translationContext) throws TranslationException {
        return translate(sentence, translationContext, 0L, 0);
    }

    public DecoderTranslation translate(String sentence, int nbest) throws TranslationException {
        return translate(sentence, null, 0L, nbest);
    }

    public DecoderTranslation translate(String sentence, long sessionId, int nbest) throws TranslationException {
        return translate(sentence, null, sessionId, nbest);
    }

    public DecoderTranslation translate(String sentence, ContextVector translationContext, int nbest) throws TranslationException {
        return translate(sentence, translationContext, 0L, nbest);
    }

    private DecoderTranslation translate(String text, ContextVector translationContext, long session, int nbest) throws TranslationException {
        TranslateOperation operation;

        if (translationContext != null) {
            operation = new TranslateOperation(text, translationContext, nbest);
        } else if (session > 0) {
            operation = new TranslateOperation(text, session, nbest);
        } else {
            operation = new TranslateOperation(text, nbest);
        }

        DecoderTranslation rootTranslation;

        try {
            rootTranslation = ModernMT.getNode().submit(operation).get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof ProcessingException)
                throw new TranslationException("Problem while processing translation", cause);
            else if (cause instanceof RuntimeException)
                throw new TranslationException("Unexpected exceptions while translating", cause);
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
                        translations[i] = translate(options[i], translationContext, session, 0);
                    }

                    mop.setTranslatedOptions(translations);
                }
            }
        }

        return rootTranslation;
    }

}
