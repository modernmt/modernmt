package eu.modernmt.facade;

import com.hazelcast.core.HazelcastException;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.TranslationTask;
import eu.modernmt.cluster.error.SystemShutdownException;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderWithNBest;
import eu.modernmt.engine.Engine;
import eu.modernmt.facade.exceptions.TimeoutException;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.impl.StringCorpus;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
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

    private static final Logger logger = LogManager.getLogger(TranslationFacade.class);

    // =============================
    //  Translation
    // =============================

    public Translation get(UUID user, LanguagePair direction, String sentence, Priority priority, long timeout) throws ProcessingException, DecoderException {
        return get(user, direction, sentence, null, 0, priority, timeout);
    }

    public Translation get(UUID user, LanguagePair direction, String sentence, ContextVector translationContext, Priority priority, long timeout) throws ProcessingException, DecoderException {
        return get(user, direction, sentence, translationContext, 0, priority, timeout);
    }

    public Translation get(UUID user, LanguagePair direction, String sentence, int nbest, Priority priority, long timeout) throws ProcessingException, DecoderException {
        return get(user, direction, sentence, null, nbest, priority, timeout);
    }

    public Translation get(UUID user, LanguagePair direction, String sentence, ContextVector translationContext, int nbest, Priority priority, long timeout) throws ProcessingException, DecoderException {
        direction = mapLanguagePair(direction);
        if (nbest > 0)
            ensureDecoderSupportsNBest();

        long expirationTimestamp = timeout > 0 ? (System.currentTimeMillis() + timeout) : 0L;

        try {
            return insecureGet(user, direction, sentence, translationContext, nbest, priority, expirationTimestamp);
        } catch (DecoderException | HazelcastException e) {
            logger.warn("Translation failed, retry after delay", e);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e1) {
                // Ignore it
            }

            return insecureGet(user, direction, sentence, translationContext, nbest, priority, expirationTimestamp);
        }
    }

    private Translation insecureGet(UUID user, LanguagePair direction, String sentence, ContextVector translationContext, int nbest, Priority priority, long expirationTimestamp) throws ProcessingException, DecoderException {
        if (expirationTimestamp > 0 && expirationTimestamp < System.currentTimeMillis())
            throw new TimeoutException();

        try {
            ClusterNode node = ModernMT.getNode();

            TranslationTask task = new TranslationTaskImpl(user, direction, sentence, translationContext, nbest, priority, expirationTimestamp);
            Future<Translation> future = node.submit(task);
            return future.get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof ProcessingException)
                throw (ProcessingException) cause;
            else if (cause instanceof DecoderException)
                throw (DecoderException) cause;
            else if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new Error("Unexpected exception thrown: " + cause.getMessage(), cause);
        }
    }

    // =============================
    //  Context Vector
    // =============================

    public ContextVector getContextVector(UUID user, LanguagePair direction, File context, int limit) throws ContextAnalyzerException {
        direction = mapLanguagePair(direction);
        return getContextVector(user, direction, new FileCorpus(context, null, direction.source), limit);
    }

    public ContextVector getContextVector(UUID user, LanguagePair direction, String context, int limit) throws ContextAnalyzerException {
        direction = mapLanguagePair(direction);
        return getContextVector(user, direction, new StringCorpus(null, direction.source, context), limit);
    }

    private ContextVector getContextVector(UUID user, LanguagePair direction, Corpus context, int limit) throws ContextAnalyzerException {
        Engine engine = ModernMT.getNode().getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        return analyzer.getContextVector(user, direction, context, limit);
    }

    public Map<Language, ContextVector> getContextVectors(UUID user, File context, int limit, Language source, Language... targets) throws ContextAnalyzerException {
        return getContextVectors(user, new FileCorpus(context, null, source), limit, source, targets);
    }

    public Map<Language, ContextVector> getContextVectors(UUID user, String context, int limit, Language source, Language... targets) throws ContextAnalyzerException {
        return getContextVectors(user, new StringCorpus(null, source, context), limit, source, targets);
    }

    private Map<Language, ContextVector> getContextVectors(UUID user, Corpus context, int limit, Language source, Language... targets) throws ContextAnalyzerException {
        Engine engine = ModernMT.getNode().getEngine();
        ContextAnalyzer analyzer = engine.getContextAnalyzer();

        HashMap<Language, ContextVector> result = new HashMap<>(targets.length);
        for (Language target : targets) {
            try {
                LanguagePair direction = mapLanguagePair(new LanguagePair(source, target));
                ContextVector contextVector = analyzer.getContextVector(user, direction, context, limit);
                result.put(target, contextVector);
            } catch (UnsupportedLanguageException e) {
                // ignore it
            }
        }

        return result;
    }

    // -----------------------------
    //  Util functions
    // -----------------------------

    private void ensureDecoderSupportsNBest() {
        Decoder decoder = ModernMT.getNode().getEngine().getDecoder();
        if (!(decoder instanceof DecoderWithNBest))
            throw new UnsupportedOperationException("Decoder '" + decoder.getClass().getSimpleName() + "' does not support N-best.");
    }

    private LanguagePair mapLanguagePair(LanguagePair pair) {
        LanguageIndex index = ModernMT.getNode().getEngine().getLanguageIndex();

        LanguagePair mapped = index.map(pair, true);
        if (mapped == null)
            throw new UnsupportedLanguageException(pair);

        return mapped;
    }

    // -----------------------------
    //  Internal Operations
    // -----------------------------

    private static class TranslationTaskImpl implements TranslationTask {

        public final UUID user;
        public final LanguagePair direction;
        public final String text;
        public final ContextVector context;
        public final int nbest;
        public final Priority priority;
        private int queueLength;
        private final long creationTimestamp;
        private final long expirationTimestamp;

        public TranslationTaskImpl(UUID user, LanguagePair direction, String text, ContextVector context, int nbest, Priority priority, long expirationTimestamp) {
            this.user = user;
            this.direction = direction;
            this.text = text;
            this.context = context;
            this.nbest = nbest;
            this.priority = priority;
            this.creationTimestamp = System.currentTimeMillis();
            this.expirationTimestamp = expirationTimestamp;
        }

        @Override
        public Translation call() throws ProcessingException, DecoderException {
            if (expirationTimestamp > 0 && expirationTimestamp < System.currentTimeMillis())
                throw new TimeoutException();

            long timeInQueue = System.currentTimeMillis() - creationTimestamp;

            ClusterNode node = ModernMT.getNode();

            Engine engine = node.getEngine();
            Decoder decoder = engine.getDecoder();
            Preprocessor preprocessor = engine.getPreprocessor();
            Postprocessor postprocessor = engine.getPostprocessor();

            Sentence sentence = preprocessor.process(direction, text);
            Translation translation;

            // Sentence splitter
            Sentence[] sentencePieces = SentenceSplitter.forLanguage(direction.source).split(sentence);
            Translation[] translationPieces = translate(sentencePieces, decoder);

            translation = this.merge(sentence, sentencePieces, translationPieces);

            postprocessor.process(direction, translation);

            // NBest list
            if (translation.hasNbest()) {
                List<Translation> hypotheses = translation.getNbest();
                postprocessor.process(direction, hypotheses);
            }

            translation.setQueueLength(queueLength);
            translation.setQueueTime(Math.max(0, timeInQueue));

            return translation;
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
                translation = nBestDecoder.translate(user, direction, sentence, context, nbest);
            } else {
                translation = decoder.translate(user, direction, sentence, context);
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

        @Override
        public void setQueueLength(int size) {
            this.queueLength = size;
        }

        @Override
        public LanguagePair getLanguage() {
            return direction;
        }

    }
}
