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
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.tags.format.InputFormat;
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

    public Set<LanguageDirection> getLanguages() {
        Engine engine = ModernMT.getNode().getEngine();
        return engine.getLanguageIndex().getLanguages();
    }

    public LanguageDirection mapLanguage(LanguageDirection pair) {
        LanguageIndex index = ModernMT.getNode().getEngine().getLanguageIndex();

        LanguageDirection mapped = index.map(pair);
        if (mapped == null)
            throw new UnsupportedLanguageException(pair);

        return mapped;
    }

    // =============================
    //  Translation
    // =============================

    public Translation get(UUID user, LanguageDirection direction, Preprocessor.Options preprocessingOptions, String text, Priority priority, long timeout
    	,String[] s,String[] t,String[] w) throws ProcessingException, DecoderException {
        return get(user, direction, preprocessingOptions, text, null, 0, priority, timeout
        	,s,t,w);
    }

    public Translation get(UUID user, LanguageDirection direction, Preprocessor.Options preprocessingOptions, String text, ContextVector translationContext, Priority priority, long timeout
    	,String[] s,String[] t,String[] w) throws ProcessingException, DecoderException {
        return get(user, direction, preprocessingOptions, text, translationContext, 0, priority, timeout
        	,s,t,w);
    }

    public Translation get(UUID user, LanguageDirection direction, Preprocessor.Options preprocessingOptions, String text, int nbest, Priority priority, long timeout
    	,String[] s,String[] t,String[] w) throws ProcessingException, DecoderException {
        return get(user, direction, preprocessingOptions, text, null, nbest, priority, timeout,s,t,w);
    }

    public Translation get(UUID user, LanguageDirection direction, Preprocessor.Options preprocessingOptions, String text, ContextVector translationContext, int nbest, Priority priority, long timeout
    	,String[] s,String[] t,String[] w) throws ProcessingException, DecoderException {
        LanguageDirection normalizedDirection = mapLanguage(direction);
        if (nbest > 0)
            ensureDecoderSupportsNBest();

        Engine engine = ModernMT.getNode().getEngine();
        Preprocessor preprocessor = engine.getPreprocessor();
        Postprocessor postprocessor = engine.getPostprocessor();

        // Pre-processing text
        Sentence sentence = preprocessor.process(normalizedDirection, text, preprocessingOptions);

        // Translating
        Translation translation;
        long expirationTimestamp = timeout > 0 ? (System.currentTimeMillis() + timeout) : 0L;

        try {
            translation = insecureGet(user, normalizedDirection, sentence, translationContext, nbest, priority, expirationTimestamp
            	,s,t,w);
        } catch (DecoderException | HazelcastException e) {
            if (e instanceof TranslationTimeoutException)
                throw e;

            logger.warn("Translation failed, retry after delay", e);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e1) {
                // Ignore it
            }

            translation = insecureGet(user, normalizedDirection, sentence, translationContext, nbest, priority, expirationTimestamp
            	,s,t,w);
        }

        // Post-processing translation
        Postprocessor.Options postprocessingOptions = new Postprocessor.Options(direction.source, direction.target);
        postprocessor.process(normalizedDirection, translation, postprocessingOptions);

        if (translation.hasNbest()) {
            for (Translation hypothesis : translation.getNbest())
                postprocessor.process(normalizedDirection, hypothesis, postprocessingOptions);
        }

        return translation;
    }

    private Translation insecureGet(UUID user, LanguageDirection direction, Sentence sentence, ContextVector context, int nbest, Priority priority, long expirationTimestamp
    	,String[] s,String[] t,String[] w) throws DecoderException {
        if (expirationTimestamp > 0 && expirationTimestamp < System.currentTimeMillis())
            throw new TranslationTimeoutException();

        if (!sentence.hasWords())
            return Translation.emptyTranslation(sentence);

        try {
            ClusterNode node = ModernMT.getNode();

            TranslationTask task = new TranslationTaskImpl(priority, user, direction, sentence, context, nbest, expirationTimestamp
            	,s,t,w);
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
        direction = mapLanguage(direction);
        return getContextVector(user, direction, new FileCorpus(context, null, direction.source), limit);
    }

    public ContextVector getContextVector(UUID user, LanguageDirection direction, String context, int limit) throws ContextAnalyzerException {
        direction = mapLanguage(direction);
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
                LanguageDirection direction = mapLanguage(new LanguageDirection(source, target));
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
        private final String[] s;
        private final String[] t;
        private final String[] w;

        TranslationTaskImpl(Priority priority, UUID user, LanguageDirection direction, Sentence sentence, ContextVector context, int nbest, long expirationTimestamp
        	,String[] s,String[] t,String[] w) {
            this.priority = priority;
            this.user = user;
            this.direction = direction;
            this.sentence = sentence;
            this.context = context;
            this.nbest = nbest;
            this.expirationTimestamp = expirationTimestamp;
            this.s = s;
            this.t = t;
            this.w = w;
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
                return nBestDecoder.translate(priority, user, direction, sentence, context, nbest, expirationTimestamp
                	,s,t,w);
            } else {
                return decoder.translate(priority, user, direction, sentence, context, expirationTimestamp
                	,s,t,w);
            }
        }

    }
}
