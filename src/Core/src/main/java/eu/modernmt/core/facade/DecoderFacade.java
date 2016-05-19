package eu.modernmt.core.facade;

import eu.modernmt.context.ContextDocument;
import eu.modernmt.core.cluster.SessionManager;
import eu.modernmt.core.cluster.error.SystemShutdownException;
import eu.modernmt.decoder.TranslationException;
import eu.modernmt.core.facade.operations.TranslateOperation;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.decoder.DecoderTranslation;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.model.MultiOptionsToken;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.framework.ProcessingException;

import java.util.HashMap;
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

    public Map<DecoderFeature, float[]> getFeatureWeights() {
        // Invoke on local decoder instance because it's just a matter of
        // properties reading and not a real computation
        Decoder decoder = ModernMT.node.getEngine().getDecoder();

        HashMap<DecoderFeature, float[]> result = new HashMap<>();
        for (DecoderFeature feature : decoder.getFeatures()) {
            float[] weights = feature.isTunable() ? decoder.getFeatureWeights(feature) : null;
            result.put(feature, weights);
        }

        return result;
    }

    public void setFeatureWeights(Map<String, float[]> weights) {
        ModernMT.node.notifyDecoderWeightsChanged(weights);
    }

    // =============================
    //  Translation session
    // =============================

    public TranslationSession openSession(List<ContextDocument> context) {
        SessionManager sessionManager = ModernMT.node.getSessionManager();
        return sessionManager.create(context);
    }

    public void closeSession(long id) {
        SessionManager sessionManager = ModernMT.node.getSessionManager();
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
            rootTranslation = ModernMT.node.submit(operation).get();
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
                        translations[i] = translate(options[i], translationContext, session, textProcessing, 0);
                    }

                    mop.setTranslatedOptions(translations);
                }
            }
        }

        return rootTranslation;
    }

}
