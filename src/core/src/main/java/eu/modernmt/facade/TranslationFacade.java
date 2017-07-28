package eu.modernmt.facade;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.decoder.*;
import eu.modernmt.engine.Engine;
import eu.modernmt.facade.exceptions.TranslationException;
import eu.modernmt.model.*;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    public Translation get(LanguagePair direction, String sentence) throws TranslationException {
        return get(new TranslateOperation(direction, sentence, null, 0));
    }

    public Translation get(LanguagePair direction, String sentence, ContextVector translationContext) throws TranslationException {
        return get(new TranslateOperation(direction, sentence, translationContext, 0));
    }

    public Translation get(LanguagePair direction, String sentence, int nbest) throws TranslationException {
        return get(new TranslateOperation(direction, sentence, null, nbest));
    }

    public Translation get(LanguagePair direction, String sentence, ContextVector translationContext, int nbest) throws TranslationException {
        return get(new TranslateOperation(direction, sentence, translationContext, nbest));
    }

    private Translation get(TranslateOperation operation) throws TranslationException {
        ensureLanguagePairIsSupported(operation.direction);

        if (operation.nbest > 0)
            ensureDecoderSupportsNBest();

        Translation translation;

        try {
            translation = ModernMT.getNode().submit(operation).get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof RuntimeException)
                throw new TranslationException("Unexpected exceptions while translating", cause);
            else if (cause instanceof ProcessingException)
                throw new TranslationException("Problem while processing translation", cause);
            else if (cause instanceof DecoderException)
                throw new TranslationException("Problem while decoding source sentence", cause);
            else if (cause instanceof AlignerException)
                throw new TranslationException("Problem while aligning source sentence to its translation", cause);
            else
                throw new Error("Unexpected exception: " + cause.getMessage(), cause);
        }

        return translation;
    }

    // =============================
    //  Languages
    // =============================

    public Set<LanguagePair> getAvailableLanguagePairs() {
        return ModernMT.getNode().getEngine().getAvailableLanguagePairs();
    }

    // =============================
    //  Context Vector
    // =============================

    public ContextVector getContextVector(LanguagePair direction, File context, int limit) throws ContextAnalyzerException {
        ensureLanguagePairIsSupported(direction);

        // Because the file is local to the machine, this method ensures that the
        // local context analyzer is invoked instead of a remote one
        Engine engine = ModernMT.getNode().getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        return analyzer.getContextVector(direction, context, limit);
    }

    public ContextVector getContextVector(LanguagePair direction, String context, int limit) throws ContextAnalyzerException {
        ensureLanguagePairIsSupported(direction);

        try {
            return ModernMT.getNode().submit(new GetContextVectorCallable(direction, context, limit)).get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof ContextAnalyzerException)
                throw (ContextAnalyzerException) cause;
            else if (cause instanceof RuntimeException)
                throw new ContextAnalyzerException("Unexpected exceptions in context analyzer", cause);
            else
                throw new Error("Unexpected exception: " + cause.getMessage(), cause);
        }
    }

    // -----------------------------
    //  Util functions
    // -----------------------------

    private void ensureDecoderSupportsNBest() {
        Decoder decoder = ModernMT.getNode().getEngine().getDecoder();
        if (!(decoder instanceof DecoderWithNBest))
            throw new UnsupportedOperationException("Decoder '" + decoder.getClass().getSimpleName() + "' does not support N-best.");
    }

    private void ensureLanguagePairIsSupported(LanguagePair pair) {
        Engine engine = ModernMT.getNode().getEngine();
        if (!engine.isLanguagePairSupported(pair))
            throw new UnsupportedLanguageException(pair);
    }

    // -----------------------------
    //  Internal Operations
    // -----------------------------

    private static class GetContextVectorCallable implements Callable<ContextVector>, Serializable {

        public final LanguagePair direction;
        private final String context;
        private final int limit;

        public GetContextVectorCallable(LanguagePair direction, String context, int limit) {
            this.direction = direction;
            this.context = context;
            this.limit = limit;
        }

        @Override
        public ContextVector call() throws ContextAnalyzerException {
            ContextAnalyzer analyzer = ModernMT.getNode().getEngine().getContextAnalyzer();
            return analyzer.getContextVector(direction, context, limit);
        }
    }

    private static class TranslateOperation implements Callable<Translation>, Serializable {

        public final LanguagePair direction;
        public final String text;
        public final ContextVector context;
        public final int nbest;

        public TranslateOperation(LanguagePair direction, String text, ContextVector context, int nbest) {
            this.direction = direction;
            this.text = text;
            this.context = context;
            this.nbest = nbest;
        }

        @Override
        public Translation call() throws ProcessingException, DecoderException, AlignerException {
            ClusterNode node = ModernMT.getNode();

            Engine engine = node.getEngine();
            Decoder decoder = engine.getDecoder();
            Preprocessor preprocessor = engine.getPreprocessor(direction);
            Postprocessor postprocessor = engine.getPostprocessor(direction);

            Sentence sentence = preprocessor.process(text);

            Translation translation;

            if (nbest > 0) {
                DecoderWithNBest nBestDecoder = (DecoderWithNBest) decoder;
                translation = nBestDecoder.translate(direction, sentence, context, nbest);
            } else {
                translation = decoder.translate(direction, sentence, context);
            }

            if (!translation.hasAlignment()) {
                Aligner aligner = engine.getAligner();
                Alignment alignment = aligner.getAlignment(direction, sentence, translation);

                translation.setAlignment(alignment);
            }

            postprocessor.process(translation);

            return translation;
        }
    }

}
