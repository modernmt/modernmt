package eu.modernmt.facade;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.TranslationTask;
import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.decoder.*;
import eu.modernmt.engine.Engine;
import eu.modernmt.facade.exceptions.TranslationException;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by davide on 31/01/17.
 */
public class TranslationFacade {

    public enum Priority {

        BACKGROUND(1000), LOW(2000), NORMAL(3000), HIGH(4000), URGENT(5000);

        public final int intValue;

        Priority(int value) {
            this.intValue = value;
        }

        public static Priority fromName(String name) {
            for (Priority priority : Priority.values())
                if (priority.name().equalsIgnoreCase(name))
                    return priority;
            throw new IllegalArgumentException("Invalid priority: " + name);
        }

    }

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

    public Translation get(LanguagePair direction, String sentence, Priority priority) throws TranslationException {
        return get(new TranslationTaskImpl(direction, sentence, null, 0, priority));
    }

    public Translation get(LanguagePair direction, String sentence, ContextVector translationContext, Priority priority) throws TranslationException {
        return get(new TranslationTaskImpl(direction, sentence, translationContext, 0, priority));
    }

    public Translation get(LanguagePair direction, String sentence, int nbest, Priority priority) throws TranslationException {
        return get(new TranslationTaskImpl(direction, sentence, null, nbest, priority));
    }

    public Translation get(LanguagePair direction, String sentence, ContextVector translationContext, int nbest, Priority priority) throws TranslationException {
        return get(new TranslationTaskImpl(direction, sentence, translationContext, nbest, priority));
    }

    private Translation get(TranslationTaskImpl task) throws TranslationException {
        ensureLanguagePairIsSupported(task.direction);

        if (task.nbest > 0)
            ensureDecoderSupportsNBest();

        try {
            ClusterNode node = ModernMT.getNode();

            Future<Translation> future = node.submit(task, task.direction);
            if (future == null)
                future = node.submit(task);

            return future.get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException(e);
        } catch (ExecutionException e) {
            throw unwrapException(TranslationException.class, e);
        }
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

        Engine engine = ModernMT.getNode().getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        return analyzer.getContextVector(direction, context, limit);
    }

    public Map<Locale, ContextVector> getContextVectors(File context, int limit, Locale source, Locale... targets) throws ContextAnalyzerException {
        List<LanguagePair> languages = filterUnsupportedLanguages(source, targets);

        if (languages.isEmpty())
            return Collections.emptyMap();

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

        Engine engine = ModernMT.getNode().getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        return analyzer.getContextVector(direction, context, limit);
    }

    public Map<Locale, ContextVector> getContextVectors(String context, int limit, Locale source, Locale... targets) throws ContextAnalyzerException {
        List<LanguagePair> languages = filterUnsupportedLanguages(source, targets);

        if (languages.isEmpty())
            return Collections.emptyMap();

        Engine engine = ModernMT.getNode().getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        HashMap<Locale, ContextVector> result = new HashMap<>(languages.size());
        for (LanguagePair direction : languages) {
            ContextVector contextVector = analyzer.getContextVector(direction, context, limit);
            result.put(direction.target, contextVector);
        }

        return result;
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

    private static class TranslationTaskImpl implements TranslationTask {
        public final LanguagePair direction;
        public final String text;
        public final ContextVector context;
        public final int nbest;
        public final Priority priority;

        public TranslationTaskImpl(LanguagePair direction, String text, ContextVector context, int nbest, Priority priority) {
            this.direction = direction;
            this.text = text;
            this.context = context;
            this.nbest = nbest;
            this.priority = priority;
        }

        @Override
        public Translation call() throws TranslationException {
            ClusterNode node = ModernMT.getNode();

            Engine engine = node.getEngine();
            Decoder decoder = engine.getDecoder();
            Preprocessor preprocessor = engine.getPreprocessor();
            Postprocessor postprocessor = engine.getPostprocessor();

            try {
                Sentence sentence = preprocessor.process(direction, text);

                Translation translation;

                if (nbest > 0) {
                    DecoderWithNBest nBestDecoder = (DecoderWithNBest) decoder;
                    translation = nBestDecoder.translate(direction, sentence, context, nbest);
                } else {
                    translation = decoder.translate(direction, sentence, context);
                }

                // Translation
                if (!translation.hasAlignment()) {
                    Aligner aligner = engine.getAligner();
                    Alignment alignment = aligner.getAlignment(direction, sentence, translation);

                    translation.setWordAlignment(alignment);
                }

                postprocessor.process(direction, translation);

                // NBest list
                if (translation.hasNbest()) {
                    List<Translation> hypotheses = translation.getNbest();

                    if (!hypotheses.get(0).hasAlignment()) {
                        ArrayList<Sentence> sources = new ArrayList<>(hypotheses.size());
                        for (int i = 0; i < hypotheses.size(); i++)
                            sources.add(sentence);

                        Aligner aligner = engine.getAligner();
                        Alignment[] alignments = aligner.getAlignments(direction, sources, hypotheses);

                        int i = 0;
                        for (Translation hypothesis : hypotheses) {
                            hypothesis.setWordAlignment(alignments[i]);
                            i++;
                        }
                    }

                    postprocessor.process(direction, hypotheses);
                }

                return translation;
            } catch (ProcessingException e) {
                throw new TranslationException("Problem while processing translation", e);
            } catch (AlignerException e) {
                throw new TranslationException("Problem while aligning source sentence to its translation", e);
            } catch (DecoderException e) {
                throw new TranslationException("Problem while decoding source sentence", e);
            }
        }

        @Override
        public int compareTo(@NotNull TranslationTask o) {
            return Integer.compare(priority.intValue, ((TranslationTaskImpl) o).priority.intValue);
        }
    }
}