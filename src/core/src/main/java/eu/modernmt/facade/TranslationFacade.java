package eu.modernmt.facade;

import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.cluster.services.IPriorityExecutorService;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.decoder.DecoderWithFeatures;
import eu.modernmt.decoder.DecoderWithNBest;
import eu.modernmt.engine.Engine;
import eu.modernmt.facade.exceptions.TranslationException;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Translation;

import java.io.File;
import java.io.Serializable;
import java.util.*;
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

    public Translation get(LanguagePair direction, String sentence, IPriorityExecutorService.Priority priority) throws TranslationException {
        return get(new TranslationTask(direction, sentence, null, 0), priority);
    }

    public Translation get(LanguagePair direction, String sentence, ContextVector translationContext, IPriorityExecutorService.Priority priority) throws TranslationException {
        return get(new TranslationTask(direction, sentence, translationContext, 0), priority);
    }

    public Translation get(LanguagePair direction, String sentence, int nbest, IPriorityExecutorService.Priority priority) throws TranslationException {
        return get(new TranslationTask(direction, sentence, null, nbest), priority);
    }

    public Translation get(LanguagePair direction, String sentence, ContextVector translationContext, int nbest, IPriorityExecutorService.Priority priority) throws TranslationException {
        return get(new TranslationTask(direction, sentence, translationContext, nbest), priority);
    }

    private Translation get(TranslationTask task, IPriorityExecutorService.Priority priority) throws TranslationException {
        Translation result;
        ensureLanguagePairIsSupported(task.direction);

        if (task.nbest > 0)
            ensureDecoderSupportsNBest();

        ClusterNode node = ModernMT.getNode();
        result = node.execute(task, task.direction, priority);
        if (result == null)
            result = node.execute(task, priority);

        return result;
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

    public Map<Locale, ContextVector> getContextVectors(File context, int limit, Locale source, Locale... targets) throws ContextAnalyzerException {
        List<LanguagePair> languages = filterUnsupportedLanguages(source, targets);

        if (languages.isEmpty())
            return Collections.emptyMap();

        // Because the file is local to the machine, this method ensures that the
        // local context analyzer is invoked instead of a remote one
        Engine engine = ModernMT.getNode().getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        HashMap<Locale, ContextVector> result = new HashMap<>(languages.size());
        for (LanguagePair direction : languages) {
            ContextVector contextVector = analyzer.getContextVector(direction, context, limit);
            result.put(direction.target, contextVector);
        }

        return result;
    }

    public ContextVector getContextVector(LanguagePair direction, String context, int limit) throws ContextAnalyzerException {
        ensureLanguagePairIsSupported(direction);

        try {
            return ModernMT.getNode().submit(new GetContextVectorCallable(new LanguagePair[]{direction}, context, limit)).get()[0];
        } catch (InterruptedException e) {
            throw new SystemShutdownException();
        } catch (ExecutionException e) {
            throw unwrapException(ContextAnalyzerException.class, e);
        }
    }

    public Map<Locale, ContextVector> getContextVectors(String context, int limit, Locale source, Locale... targets) throws ContextAnalyzerException {
        List<LanguagePair> _languages = filterUnsupportedLanguages(source, targets);

        if (_languages.isEmpty())
            return Collections.emptyMap();

        try {
            LanguagePair[] languages = _languages.toArray(new LanguagePair[_languages.size()]);
            ContextVector[] vectors = ModernMT.getNode().submit(new GetContextVectorCallable(languages, context, limit)).get();

            HashMap<Locale, ContextVector> result = new HashMap<>(languages.length);

            for (int i = 0; i < vectors.length; i++)
                result.put(languages[i].target, vectors[i]);

            return result;
        } catch (InterruptedException e) {
            throw new SystemShutdownException();
        } catch (ExecutionException e) {
            throw unwrapException(ContextAnalyzerException.class, e);
        }
    }

    // -----------------------------
    //  Util functions
    // -----------------------------

    private <T extends Throwable> T unwrapException(Class<T> type, ExecutionException e) {
        Throwable cause = e.getCause();

        if (type.isAssignableFrom(cause.getClass()))
            return type.cast(cause);
        else if (cause instanceof RuntimeException)
            throw (RuntimeException) cause;
        else
            throw new Error("Unexpected exception: " + cause.getMessage(), cause);
    }

    private void ensureDecoderSupportsNBest() {
        Decoder decoder = ModernMT.getNode().getEngine().getDecoder();
        if (!(decoder instanceof DecoderWithNBest))
            throw new UnsupportedOperationException("Decoder '" + decoder.getClass().getSimpleName() + "' does not support N-best.");
    }

    private void ensureLanguagePairIsSupported(LanguagePair pair) {
        LanguageIndex languages = ModernMT.getNode().getEngine().getLanguages();
        if (!languages.isSupported(pair))
            throw new UnsupportedLanguageException(pair);
    }

    private List<LanguagePair> filterUnsupportedLanguages(Locale source, Locale[] targets) {
        ArrayList<LanguagePair> result = new ArrayList<>(targets.length);

        LanguageIndex languages = ModernMT.getNode().getEngine().getLanguages();
        for (Locale target : targets) {
            LanguagePair language = new LanguagePair(source, target);

            if (languages.isSupported(language))
                result.add(language);
        }

        return result;
    }

    // -----------------------------
    //  Internal Operations
    // -----------------------------

    private static class GetContextVectorCallable implements Callable<ContextVector[]>, Serializable {

        public final LanguagePair[] languages;
        private final String context;
        private final int limit;

        public GetContextVectorCallable(LanguagePair[] languages, String context, int limit) {
            this.languages = languages;
            this.context = context;
            this.limit = limit;
        }

        @Override
        public ContextVector[] call() throws ContextAnalyzerException {
            ContextAnalyzer analyzer = ModernMT.getNode().getEngine().getContextAnalyzer();

            ContextVector[] result = new ContextVector[languages.length];
            for (int i = 0; i < result.length; i++)
                result[i] = analyzer.getContextVector(languages[i], context, limit);

            return result;
        }
    }

    public static class TranslationTask implements Serializable{
        public final LanguagePair direction;
        public final String text;
        public final ContextVector context;
        public final int nbest;

        public TranslationTask(LanguagePair direction, String text, ContextVector context, int nbest) {
            this.direction = direction;
            this.text = text;
            this.context = context;
            this.nbest = nbest;
        }
    }
}
