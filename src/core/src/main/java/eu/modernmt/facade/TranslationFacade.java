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
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.*;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.splitter.SentenceSplitter;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by davide on 31/01/17.
 */
public class TranslationFacade {

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
            throw unwrapException(e);
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

                if (decoder.supportsSentenceSplit()) {
                    /* split the Sentence tokens in multiple sentences using the SentenceSplitter */
                    Sentence[] splitSentences = SentenceSplitter.forLanguage(direction.source).split(sentence);
                    /*get the corresponding translations*/
                    Translation[] splitTranslations = translate(splitSentences, engine);
                    /*merge them back*/
                    translation = this.merge(sentence, splitSentences, splitTranslations);
                } else {
                    translation = translate(sentence, engine);
                }

                // Alignment
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

        /**
         * This private method asks the passed Engine to translate one single sentence
         * @param sentence the Sentence object to translate
         * @param engine the engine to which the translation must be requested
         * @return the obtained Translation
         * @throws DecoderException if there is an error in the Decoding process
         * @throws AlignerException if there is an error while computing the alignments
         */
        private Translation translate(Sentence sentence, Engine engine) throws DecoderException, AlignerException{
            Decoder decoder = engine.getDecoder();

            Translation translation;

            if (nbest > 0) {
                DecoderWithNBest nBestDecoder = (DecoderWithNBest) decoder;
                translation = nBestDecoder.translate(direction, sentence, context, nbest);
            } else {
                translation = decoder.translate(direction, sentence, context);
            }
            return translation;
        }

        /**
         * This private method asks the passed Engine to translate, one by one, the passed sentences
         * @param sentences an array containing the Sentence objects to translate
         * @param engine the engine to which the translations must be requested
         * @return an array containing, for each passed Sentence, the corresponding Translation
         * @throws DecoderException if there is an error in the Decoding process
         * @throws AlignerException if there is an error while computing the alignments
         */
        private Translation[] translate(Sentence[] sentences, Engine engine) throws DecoderException, AlignerException{

            Translation[] translations = new Translation[sentences.length];

            for(int i = 0; i < sentences.length; i++)
                translations[i] = this.translate(sentences[i], engine);

            return translations;
        }

        /**
         * This private method merges a group of split translations back into a single Translation.
         * If necessary, it also takes into account the Translation NBests.
         *
         * This method is typically called by the TranslationTaskImpl "translate" method when
         * the original sentence was split into multiple sentences and it needs
         * It is first used on the obtained split translation and then, if those translation had NBests too,
         * it is also called iteratively on their NBests to merge them too.
         *
         * @param originalSentence the original Sentence to translate
         * @param splitSentences the sub-sentences obtained by splitting the first Sentence
         * @param splitTranslations the sub-translations obtained by translating the sub-sentences one by one
         * @return the Translation obtained by merging the sub-translations back into one
         */
        private Translation merge(Sentence originalSentence, Sentence[] splitSentences, Translation[] splitTranslations) {

            Translation translation = this.join(originalSentence, splitSentences, splitTranslations);

            /* If there are nbests in the obtained translations, they must be merged too.
            Join the n-bests in the array to obtain a valid nbest for the global translation.*/
            if (!splitTranslations[0].hasNbest())
                return translation;

            int nbestSize = splitTranslations[0].getNbest().size();
            if (nbestSize > 0) {
                List<Translation> globalNBests = new ArrayList<>(nbestSize);    //list of global merged nbests to write in the result global translation
                Translation[] nbests = new Translation[splitTranslations.length];   //array with the nbests with same nbest index got from the partial translations

                /* Iterate over the n-bests of each translation and recompute in each iteration the nbest array
                to have the partial nbests referring to the current nbest index (e.g. the 2nd nbest from all partial translations)
                Then merge the Translations in the recomputed nbests array and put the result in globalNBests*/

                for (int nbestIndex = 0; nbestIndex < nbestSize; nbestIndex++) {
                    for (int translationIndex = 0; translationIndex < splitTranslations.length; translationIndex++)
                        nbests[translationIndex] = splitTranslations[translationIndex].getNbest().get(nbestIndex);
                    globalNBests.add(this.join(originalSentence, splitSentences, nbests));
                }

                translation.setNbest(globalNBests);
            }

            return translation;
        }

        /**
         * This private method merges a group of split translations back into a single Translation
         * without taking into account the Translation NBests.
         *
         * This method is typically called by the TranslationTaskImpl "merge" method when
         * the original sentence was split into multiple sentences and so multiple translations were obtained.
         * It is first used on the obtained split translation and then, if those translation had NBests too,
         * it is also called iteratively on their NBests to merge them too.
         *
         * @param originalSentence the original Sentence to translate
         * @param splitSentences the sub-sentences obtained by splitting the first Sentence
         * @param splitTranslations the sub-translations obtained by translating the sub-sentences one by one
         * @return the Translation obtained by merging the sub-translations back into one
         */
        private Translation join(Sentence originalSentence, Sentence[] splitSentences, Translation[] splitTranslations) {

            /* get the sizes for the data structures of the "global" translation: wordsSize, wordAlignmentSize.
            * [do not use tags because they are still projected in the translation */
            int globalWordsSize = 0;
            int globalWordAlignmentSize = 0;
            for (Translation splitTranslation : splitTranslations) {
                globalWordsSize += splitTranslation.getWords().length;
                if (splitTranslations[0].hasAlignment())
                    globalWordAlignmentSize += splitTranslation.getWordAlignment().size();  //remains 0 if translations do not have alignments
            }

            /* create and initialize the data structures for the global translation using the computed sizes */
            long globalElapsedTime = 0L;
            Word[] globalWords = new Word[globalWordsSize];
            int[] globalSrcIndexes = new int[globalWordAlignmentSize];    //size 0 if translations do not have alignments
            int[] globalTrgIndexes = new int[globalWordAlignmentSize];    //size 0 if translations do not have alignments

            /* for each partial translation get words and alignment indexes and  merge them in the right positions of the global translation arrays */
            int srcWordsOffset = 0;  /* in each iteration, this is the amount of src words seen in the previous sentences*/
            int trgWordsOffset = 0; /* in each iteration, this is the amount of trg words seen in the previous sentences*/
            /* this is the latest visited position in the global indexes in the last iteration*/
            int latestGlobalPosition = 0;
            for (int sentenceIndex = 0; sentenceIndex < splitTranslations.length; sentenceIndex++) {
                Translation splitTranslation = splitTranslations[sentenceIndex];
                Sentence splitSentence = splitSentences[sentenceIndex];

                /* add the partial elapsed time to the global one */
                globalElapsedTime += splitTranslation.getElapsedTime();

                /*merge alignments if alignment must be considered*/
                if (splitTranslation.hasAlignment()) {
                    int[] localSrcIndexes = splitTranslation.getWordAlignment().getSourceIndexes();   // possibly not initialized
                    int[] localTrgIndexes = splitTranslation.getWordAlignment().getTargetIndexes();   // possibly not initialized
                    for (int localIndex = 0; localIndex < localSrcIndexes.length; localIndex++)
                        globalSrcIndexes[localIndex + latestGlobalPosition] = localSrcIndexes[localIndex] + srcWordsOffset;
                    for (int localIndex = 0; localIndex < localTrgIndexes.length; localIndex++)
                        globalTrgIndexes[localIndex + latestGlobalPosition] = localTrgIndexes[localIndex] + trgWordsOffset;

                    latestGlobalPosition += localSrcIndexes.length;
                }

                /*merge words*/
                Word[] localWords = splitTranslation.getWords();
                System.arraycopy(localWords, 0, globalWords, trgWordsOffset, localWords.length);

                srcWordsOffset += splitSentence.length();
                trgWordsOffset += splitTranslation.length();
            }

            Alignment globalAlignment = new Alignment(globalSrcIndexes, globalTrgIndexes);

            Translation globalTranslation = new Translation(globalWords, originalSentence, globalAlignment);
            globalTranslation.setElapsedTime(globalElapsedTime);
            return globalTranslation;
        }

        @Override
        public int getPriority() {
            return this.priority.intValue;
        }
    }
}