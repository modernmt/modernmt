package eu.modernmt.facade;

import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.decoder.*;
import eu.modernmt.engine.Engine;
import eu.modernmt.facade.exceptions.TranslationException;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.MultiOptionsToken;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.ProcessingException;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Created by davide on 31/01/17.
 */
public class TranslationFacade {

    // =============================
    //  Decoder Weights
    // =============================

    private DecoderWithFeatures getDecoderWithFeatures() {
        Decoder decoder = ModernMT.getNode().getEngine().getDecoder();
        if (!(decoder instanceof DecoderWithFeatures))
            throw new UnsupportedOperationException("Decoder '" + decoder.getClass().getSimpleName() + "' does not support features.");
        return (DecoderWithFeatures) decoder;
    }

    public Map<DecoderFeature, float[]> getDecoderWeights() {
        // Invoke on local decoder instance because it's just a matter of
        // properties reading and not a real computation
        DecoderWithFeatures decoder = getDecoderWithFeatures();

        HashMap<DecoderFeature, float[]> result = new HashMap<>();
        for (DecoderFeature feature : decoder.getFeatures()) {
            float[] weights = feature.isTunable() ? decoder.getFeatureWeights(feature) : null;
            result.put(feature, weights);
        }

        return result;
    }

    public void setDecoderWeights(Map<String, float[]> weights) {
        getDecoderWithFeatures(); // Ensure decoder supports features
        ModernMT.getNode().notifyDecoderWeightsChanged(weights);
    }

    // =============================
    //  Translation
    // =============================

    private void ensureDecoderSupportsNBest() {
        Decoder decoder = ModernMT.getNode().getEngine().getDecoder();
        if (!(decoder instanceof DecoderWithNBest))
            throw new UnsupportedOperationException("Decoder '" + decoder.getClass().getSimpleName() + "' does not support N-best.");
    }

    public Translation get(String sentence) throws TranslationException {
        return get(new TranslateOperation(sentence, null, 0));
    }

    public Translation get(String sentence, ContextVector translationContext) throws TranslationException {
        return get(new TranslateOperation(sentence, translationContext, 0));
    }

    public Translation get(String sentence, int nbest) throws TranslationException {
        if (nbest > 0)
            ensureDecoderSupportsNBest();
        return get(new TranslateOperation(sentence, null, nbest));
    }

    public Translation get(String sentence, ContextVector translationContext, int nbest) throws TranslationException {
        if (nbest > 0)
            ensureDecoderSupportsNBest();
        return get(new TranslateOperation(sentence, translationContext, nbest));
    }

    private Translation get(TranslateOperation operation) throws TranslationException {
        Translation rootTranslation;

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
            else if (cause instanceof DecoderException)
                throw new TranslationException("Problem while decoding source sentence", cause);
            else
                throw new Error("Unexpected exception: " + cause.getMessage(), cause);
        }

        for (Token token : rootTranslation) {
            if (token instanceof MultiOptionsToken) {
                MultiOptionsToken mop = (MultiOptionsToken) token;

                if (!mop.hasTranslatedOptions()) {
                    String[] options = mop.getSourceOptions();
                    Translation[] translations = new Translation[options.length];

                    for (int i = 0; i < translations.length; i++)
                        translations[i] = get(new TranslateOperation(options[i], operation.context, 0));

                    mop.setTranslatedOptions(translations);
                }
            }
        }

        return rootTranslation;
    }

    // =============================
    //  Languages
    // =============================

    public Locale getSourceLanguage() {
        return ModernMT.getNode().getEngine().getSourceLanguage();
    }

    public Locale getTargetLanguage() {
        return ModernMT.getNode().getEngine().getTargetLanguage();
    }

    // =============================
    //  Context Vector
    // =============================

    public ContextVector getContextVector(File context, int limit) throws ContextAnalyzerException {
        // Because the file is local to the machine, this method ensures that the
        // local context analyzer is invoked instead of a remote one
        Engine engine = ModernMT.getNode().getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        return analyzer.getContextVector(context, limit);
    }

    public ContextVector getContextVector(String context, int limit) throws ContextAnalyzerException {
        try {
            return ModernMT.getNode().submit(new GetContextVectorCallable(context, limit)).get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException();
        } catch (ExecutionException e) {
            throw unwrap(e);
        }
    }

    private static class GetContextVectorCallable implements Callable<ContextVector>, Serializable {

        private final String context;
        private final int limit;

        public GetContextVectorCallable(String context, int limit) {
            this.context = context;
            this.limit = limit;
        }

        @Override
        public ContextVector call() throws ContextAnalyzerException {
            ContextAnalyzer analyzer = ModernMT.getNode().getEngine().getContextAnalyzer();
            return analyzer.getContextVector(context, limit);
        }
    }

    private static ContextAnalyzerException unwrap(ExecutionException e) {
        Throwable cause = e.getCause();

        if (cause instanceof ContextAnalyzerException)
            return (ContextAnalyzerException) cause;
        else if (cause instanceof RuntimeException)
            return new ContextAnalyzerException("Unexpected exceptions in context analyzer", cause);
        else
            throw new Error("Unexpected exception: " + cause.getMessage(), cause);
    }

}
