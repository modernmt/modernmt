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
import eu.modernmt.decoder.TranslationTimeoutException;
import eu.modernmt.engine.Engine;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Priority;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.impl.StringCorpus;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by davide on 31/01/17.
 */
public class TranslationFacade {

    private static final Logger logger = LogManager.getLogger(TranslationFacade.class);

    // =============================
    //  Translation
    // =============================

    public Translation get(UUID user, LanguageDirection direction, String text, Priority priority, long timeout) throws ProcessingException, DecoderException {
        return get(user, direction, text, null, 0, priority, timeout);
    }

    public Translation get(UUID user, LanguageDirection direction, String text, ContextVector translationContext, Priority priority, long timeout) throws ProcessingException, DecoderException {
        return get(user, direction, text, translationContext, 0, priority, timeout);
    }

    public Translation get(UUID user, LanguageDirection direction, String text, int nbest, Priority priority, long timeout) throws ProcessingException, DecoderException {
        return get(user, direction, text, null, nbest, priority, timeout);
    }

    public Translation get(UUID user, LanguageDirection direction, String text, ContextVector translationContext, int nbest, Priority priority, long timeout) throws ProcessingException, DecoderException {
        direction = mapLanguagePair(direction);
        if (nbest > 0)
            ensureDecoderSupportsNBest();

        Engine engine = ModernMT.getNode().getEngine();
        Preprocessor preprocessor = engine.getPreprocessor();
        Postprocessor postprocessor = engine.getPostprocessor();

        // Pre-processing text
        Sentence sentence = preprocessor.process(direction, text);

        // Translating
        Translation translation;
        long expirationTimestamp = timeout > 0 ? (System.currentTimeMillis() + timeout) : 0L;

        try {
            translation = insecureGet(user, direction, sentence, translationContext, nbest, priority, expirationTimestamp);
        } catch (DecoderException | HazelcastException e) {
            if (e instanceof TranslationTimeoutException)
                throw e;

            logger.warn("Translation failed, retry after delay", e);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e1) {
                // Ignore it
            }

            translation = insecureGet(user, direction, sentence, translationContext, nbest, priority, expirationTimestamp);
        }

        // Post-processing translation
        postprocessor.process(direction, translation);

        if (translation.hasNbest()) {
            List<Translation> hypotheses = translation.getNbest();
            postprocessor.process(direction, hypotheses);
        }

        return translation;
    }

    private Translation insecureGet(UUID user, LanguageDirection direction, Sentence sentence, ContextVector context, int nbest, Priority priority, long expirationTimestamp) throws DecoderException {
        if (expirationTimestamp > 0 && expirationTimestamp < System.currentTimeMillis())
            throw new TranslationTimeoutException();

        try {
            ClusterNode node = ModernMT.getNode();

            TranslationTask task = new TranslationTaskImpl(priority, user, direction, sentence, context, nbest, expirationTimestamp);
            Future<Translation> future = node.submit(task);
            return future.get();
        } catch (InterruptedException e) {
            throw new SystemShutdownException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof DecoderException)
                throw (DecoderException) cause;
            else if (cause instanceof RejectedExecutionException)
                throw new SystemShutdownException(cause);
            else if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new Error("Unexpected exception thrown: " + cause.getMessage(), cause);
        }
    }

    // =============================
    //  Context Vector
    // =============================

    public ContextVector getContextVector(UUID user, LanguageDirection direction, File context, int limit) throws ContextAnalyzerException {
        direction = mapLanguagePair(direction);
        return getContextVector(user, direction, new FileCorpus(context, null, direction.source), limit);
    }

    public ContextVector getContextVector(UUID user, LanguageDirection direction, String context, int limit) throws ContextAnalyzerException {
        direction = mapLanguagePair(direction);
        return getContextVector(user, direction, new StringCorpus(null, direction.source, context), limit);
    }

    private ContextVector getContextVector(UUID user, LanguageDirection direction, Corpus context, int limit) throws ContextAnalyzerException {
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
                LanguageDirection direction = mapLanguagePair(new LanguageDirection(source, target));
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

    private LanguageDirection mapLanguagePair(LanguageDirection pair) {
        LanguageIndex index = ModernMT.getNode().getEngine().getLanguageIndex();

        LanguageDirection mapped = index.map(pair);
        if (mapped == null)
            throw new UnsupportedLanguageException(pair);

        return mapped;
    }

    // -----------------------------
    //  Translation task
    // -----------------------------

    private static class TranslationTaskImpl implements TranslationTask {

        private final Priority priority;
        private final UUID user;
        private final LanguageDirection direction;
        private final Sentence sentence;
        private final ContextVector context;
        private final int nbest;

        private final long expirationTimestamp;

        TranslationTaskImpl(Priority priority, UUID user, LanguageDirection direction, Sentence sentence, ContextVector context, int nbest, long expirationTimestamp) {
            this.priority = priority;
            this.user = user;
            this.direction = direction;
            this.sentence = sentence;
            this.context = context;
            this.nbest = nbest;
            this.expirationTimestamp = expirationTimestamp;
        }

        @Override
        public LanguageDirection getLanguageDirection() {
            return direction;
        }

        @Override
        public Translation call() throws DecoderException {
            if (expirationTimestamp > 0 && expirationTimestamp < System.currentTimeMillis())
                throw new TranslationTimeoutException();

            Decoder decoder = ModernMT.getNode().getEngine().getDecoder();

            if (nbest > 0) {
                DecoderWithNBest nBestDecoder = (DecoderWithNBest) decoder;
                return nBestDecoder.translate(priority, user, direction, sentence, context, nbest, expirationTimestamp);
            } else {
                return decoder.translate(priority, user, direction, sentence, context, expirationTimestamp);
            }
        }

    }
}
