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
import eu.modernmt.facade.exceptions.TranslationRejectedException;
import eu.modernmt.lang.Language;
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
import eu.modernmt.processing.splitter.SentenceSplitter;
import eu.modernmt.processing.splitter.TranslationJoiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by davide on 31/01/17.
 */
public class TranslationFacade {

    private static final Logger logger = LogManager.getLogger(TranslationFacade.class);
    private LanguagePair lastTranslationLanguage = null;

    public enum Priority {
        HIGH(0), NORMAL(1), BACKGROUND(2);  //three priority values are allowed

        public final int intValue;

        Priority(int value) {
            this.intValue = value;
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

    public Translation get(LanguagePair direction, String sentence, Priority priority, String variant) throws TranslationException {
        return get(new TranslationTaskImpl(direction, sentence, null, 0, priority, variant));
    }

    public Translation get(LanguagePair direction, String sentence, ContextVector translationContext, Priority priority, String variant) throws TranslationException {
        return get(new TranslationTaskImpl(direction, sentence, translationContext, 0, priority, variant));
    }

    public Translation get(LanguagePair direction, String sentence, int nbest, Priority priority, String variant) throws TranslationException {
        return get(new TranslationTaskImpl(direction, sentence, null, nbest, priority, variant));
    }

    public Translation get(LanguagePair direction, String sentence, ContextVector translationContext, int nbest, Priority priority, String variant) throws TranslationException {
        return get(new TranslationTaskImpl(direction, sentence, translationContext, nbest, priority, variant));
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

            Translation translation = future.get();

            if (logger.isDebugEnabled())
                logger.debug("Translation of " + translation.getSource().length() + " words took " + (((double) translation.getElapsedTime()) / 1000.) + "s");

            return translation;
        } catch (InterruptedException e) {
            throw new SystemShutdownException(e);
        } catch (ExecutionException e) {
            throw unwrapException(e);
        }
    }

    public void test() throws TranslationException {
        LanguagePair language = selectForTest();
        String text = "Translation test " + new Random().nextInt();


        TranslationTaskImpl task = new TranslationTaskImpl(language, text, null, 0, TranslationFacade.Priority.HIGH, null);
        Translation translation = task.call();
        if (!translation.hasWords())
            throw new TranslationException("Empty translation for test sentence '" + text + "'");
    }

    private LanguagePair selectForTest() {
        LanguagePair language = getLastTranslationLanguage();

        if (language == null) {
            LanguageIndex index = ModernMT.getNode().getEngine().getLanguages();

            for (LanguagePair pair : index.getLanguages()) {
                if ("en".equalsIgnoreCase(pair.source.getLanguage()))
                    return pair;
            }

            language = index.getLanguages().iterator().next();
        }

        return language;
    }

    private synchronized LanguagePair getLastTranslationLanguage() {
        return this.lastTranslationLanguage;
    }

    private synchronized void setLastTranslationLanguage(LanguagePair lastTranslationLanguage) {
        this.lastTranslationLanguage = lastTranslationLanguage;
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

    public Map<Language, ContextVector> getContextVectors(File context, int limit, Language source, Language... targets) throws ContextAnalyzerException {
        List<LanguagePair> languages = filterUnsupportedLanguages(source, targets);

        if (languages.isEmpty())
            return Collections.emptyMap();

        Engine engine = ModernMT.getNode().getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        HashMap<Language, ContextVector> result = new HashMap<>(languages.size());
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

    public Map<Language, ContextVector> getContextVectors(String context, int limit, Language source, Language... targets) throws ContextAnalyzerException {
        List<LanguagePair> languages = filterUnsupportedLanguages(source, targets);

        if (languages.isEmpty())
            return Collections.emptyMap();

        Engine engine = ModernMT.getNode().getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        HashMap<Language, ContextVector> result = new HashMap<>(languages.size());
        for (LanguagePair direction : languages) {
            ContextVector contextVector = analyzer.getContextVector(direction, context, limit);
            result.put(direction.target, contextVector);
        }

        return result;
    }

    // -----------------------------
    //  Util functions
    // -----------------------------

    private TranslationException unwrapException(ExecutionException e) {
        Throwable cause = e.getCause();

        if (cause instanceof TranslationException)
            return (TranslationException) cause;
        else if (cause instanceof RejectedExecutionException)
            return new TranslationRejectedException();
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
        if (!languages.contains(pair))
            throw new UnsupportedLanguageException(pair);
    }

    private List<LanguagePair> filterUnsupportedLanguages(Language source, Language[] targets) {
        ArrayList<LanguagePair> result = new ArrayList<>(targets.length);

        LanguageIndex languages = ModernMT.getNode().getEngine().getLanguages();
        for (Language target : targets) {
            LanguagePair language = new LanguagePair(source, target);

            if (languages.contains(language))
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
        public final String variant;

        public TranslationTaskImpl(LanguagePair direction, String text, ContextVector context, int nbest, Priority priority, String variant) {
            this.direction = direction;
            this.text = text;
            this.context = context;
            this.nbest = nbest;
            this.priority = priority;
            this.variant = variant;
        }

        @Override
        public Translation call() throws TranslationException {
            ModernMT.translation.setLastTranslationLanguage(direction);

            ClusterNode node = ModernMT.getNode();

            Engine engine = node.getEngine();
            Decoder decoder = engine.getDecoder();
            Preprocessor preprocessor = engine.getPreprocessor();
            Postprocessor postprocessor = engine.getPostprocessor();

            try {
                long begin = System.currentTimeMillis();

                Sentence sentence = preprocessor.process(direction, text);
                Translation translation;

                if (decoder.supportsSentenceSplit()) {
                    Sentence[] sentencePieces = SentenceSplitter.forLanguage(direction.source).split(sentence);
                    Translation[] translationPieces = translate(sentencePieces, decoder);

                    translation = this.merge(sentence, sentencePieces, translationPieces);
                } else {
                    translation = translate(sentence, decoder);
                }

                // Alignment
                if (!translation.hasAlignment()) {
                    Aligner aligner = engine.getAligner();

                    Alignment alignment = aligner.getAlignment(direction, sentence, translation);
                    translation.setWordAlignment(alignment);

                    if (translation.hasNbest()) {
                        for (Translation nbest : translation.getNbest()) {
                            Alignment nbestAlignment = aligner.getAlignment(direction, sentence, nbest);
                            nbest.setWordAlignment(nbestAlignment);
                        }
                    }
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

                translation.setElapsedTime(System.currentTimeMillis() - begin);

                return translation;
            } catch (ProcessingException e) {
                throw new TranslationException("Problem while processing translation", e);
            } catch (AlignerException e) {
                throw new TranslationException("Problem while aligning source sentence to its translation", e);
            } catch (DecoderException e) {
                throw new TranslationException("Problem while decoding source sentence", e);
            }
        }

        private Translation[] translate(Sentence[] sentences, Decoder decoder) throws DecoderException {
            Translation[] translations = new Translation[sentences.length];

            for (int i = 0; i < sentences.length; i++)
                translations[i] = this.translate(sentences[i], decoder);

            return translations;
        }

        private Translation translate(Sentence sentence, Decoder decoder) throws DecoderException {
            Translation translation;

            if (nbest > 0) {
                DecoderWithNBest nBestDecoder = (DecoderWithNBest) decoder;
                translation = nBestDecoder.translate(direction, variant, sentence, context, nbest);
            } else {
                translation = decoder.translate(direction, variant, sentence, context);
            }

            return translation;
        }

        private Translation merge(Sentence originalSentence, Sentence[] sentencePieces, Translation[] translationPieces) {
            Translation translation = TranslationJoiner.join(originalSentence, sentencePieces, translationPieces);

            if (translation.hasNbest()) {
                int nbestSize = 0;
                for (Translation piece : translationPieces)
                    nbestSize = Math.max(nbestSize, piece.getNbest().size());

                List<Translation> globalNBests = new ArrayList<>(nbestSize);
                Translation[] ithNBests = new Translation[translationPieces.length];

                for (int i = 0; i < nbestSize; i++) {
                    for (int t = 0; t < translationPieces.length; t++) {
                        Translation piece = translationPieces[t];
                        int index = Math.min(i, piece.length() - 1);  // If not enough options, take the last one

                        ithNBests[t] = piece.getNbest().get(index);
                    }

                    globalNBests.add(TranslationJoiner.join(originalSentence, sentencePieces, ithNBests));
                }

                translation.setNbest(globalNBests);
            }

            return translation;
        }

        @Override
        public int getPriority() {
            return this.priority.intValue;
        }
    }
}