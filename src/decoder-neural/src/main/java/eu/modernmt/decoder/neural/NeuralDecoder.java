package eu.modernmt.decoder.neural;

import eu.modernmt.config.DecoderConfig;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.decoder.*;
import eu.modernmt.decoder.neural.execution.DecoderQueue;
import eu.modernmt.decoder.neural.execution.PythonDecoder;
import eu.modernmt.decoder.neural.execution.impl.DecoderQueueImpl;
import eu.modernmt.decoder.neural.execution.impl.PythonDecoderImpl;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.memory.TranslationMemory;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.splitter.SentenceSplitter;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 22/05/17.
 */
public class NeuralDecoder extends Decoder implements DecoderWithNBest, DataListenerProvider {

    private static File getJarPath() throws DecoderException {
        URL url = Decoder.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new DecoderException("Unable to resolve JAR file", e);
        }
    }

    private final Logger logger = LogManager.getLogger(getClass());

    private final boolean echoServer;
    private final int suggestionsLimit;
    private final TranslationMemory memory;
    private final Set<LanguageDirection> directions;
    private final DecoderQueue decoderQueue;

    private volatile long lastSuccessfulTranslation = 0L;

    public NeuralDecoder(File model, DecoderConfig config) throws DecoderException {
        super(model, config);

        // Load ModelConfig
        ModelConfig modelConfig;
        try {
            modelConfig = this.loadModelConfig(new File(model, "model.conf"));
        } catch (IOException e) {
            throw new DecoderException("Failed to read file model.conf", e);
        }

        this.suggestionsLimit = modelConfig.getSuggestionsLimit();
        this.directions = new HashSet<>(modelConfig.getAvailableModels().keySet());
        this.echoServer = modelConfig.isEchoServer();

        // Translation Memory
        try {
            this.memory = loadTranslationMemory(modelConfig, new File(model, "memory"));
        } catch (IOException e) {
            throw new DecoderException("Failed to initialize memory", e);
        }

        // Decoder Queue
        this.decoderQueue = this.echoServer ? null : loadDecoderQueue(modelConfig, config, model);
    }

    protected ModelConfig loadModelConfig(File filepath) throws IOException {
        return ModelConfig.load(filepath);
    }

    protected TranslationMemory loadTranslationMemory(ModelConfig config, File model) throws IOException {
        return new LuceneTranslationMemory(model, config.getQueryMinimumResults());
    }

    protected DecoderQueue loadDecoderQueue(ModelConfig modelConfig, DecoderConfig decoderConfig, File model) throws DecoderException {
        PythonDecoder.Builder builder = new PythonDecoderImpl.Builder(getJarPath(), model);

        if (decoderConfig.isUsingGPUs())
            return DecoderQueueImpl.newGPUInstance(modelConfig, builder, decoderConfig.getGPUs());
        else
            return DecoderQueueImpl.newCPUInstance(modelConfig, builder, decoderConfig.getThreads());
    }

    public boolean isEchoServer() {
        return echoServer;
    }

    // Decoder

    @Override
    public TranslationMemory getTranslationMemory() {
        return memory;
    }

    @Override
    public void setListener(DecoderListener listener) {
        if (decoderQueue != null)
            decoderQueue.setListener(listener);
        listener.onTranslationDirectionsChanged(directions);
    }

    @Override
    public Translation translate(UUID user, LanguageDirection direction, Sentence text) throws DecoderException {
        return translate(user, direction, text, null, 0);
    }

    @Override
    public Translation translate(UUID user, LanguageDirection direction, Sentence text, int nbestListSize) throws DecoderException {
        return translate(user, direction, text, null, nbestListSize);
    }

    @Override
    public Translation translate(UUID user, LanguageDirection direction, Sentence text, ContextVector contextVector) throws DecoderException {
        return translate(user, direction, text, contextVector, 0);
    }

    @Override
    public Translation translate(UUID user, LanguageDirection direction, Sentence text, ContextVector contextVector, int nbestListSize) throws DecoderException {
        if (!this.directions.contains(direction))
            throw new UnsupportedLanguageException(direction);

        // Preparing translation jobs
        SentenceSplitter splitter = new SentenceSplitter(text);
        Sentence[] textSplits = splitter.split();
        TranslationJob[] jobs;

        if (textSplits == null) {
            jobs = new TranslationJob[]{new TranslationJob(this, user, direction, text, contextVector)};
        } else {
            jobs = new TranslationJob[textSplits.length];
            for (int i = 0; i < textSplits.length; i++)
                jobs[i] = new TranslationJob(this, user, direction, textSplits[i], contextVector);
        }

        // Search for suggestions
        long lookupTime = 0L;
        for (TranslationJob job : jobs)
            lookupTime += job.computeSuggestions(this.suggestionsLimit);

        // Translate sentence pieces
        long decodeTime = 0L;

        PythonDecoder decoder = null;

        try {
            decoder = echoServer ? null : decoderQueue.take(direction);

            for (TranslationJob job : jobs)
                lookupTime += job.computeTranslation(decoder);
        } finally {
            if (decoder != null)
                decoderQueue.release(decoder);
        }

        // Collect translation splits
        Translation[] translations = new Translation[jobs.length];

        for (int i = 0; i < jobs.length; i++) {
            if (logger.isDebugEnabled()) {
                Translation translation = jobs[i].getTranslation();
                ScoreEntry[] suggestions = jobs[i].getSuggestions();

                String sourceText = TokensOutputStream.serialize(text, false, true);
                String targetText = TokensOutputStream.serialize(translation, false, true);

                StringBuilder log = new StringBuilder("Translation received from neural decoder:\n" +
                        "   sentence = " + sourceText + "\n" +
                        "   translation = " + targetText + "\n" +
                        "   suggestions = [\n");

                if (suggestions != null && suggestions.length > 0) {
                    for (ScoreEntry entry : suggestions)
                        log.append("      ").append(entry).append('\n');
                }

                log.append("   ]");

                logger.debug(log);
            }

            translations[i] = jobs[i].getTranslation();
        }

        // Merge translation splits
        Translation translation;

        if (textSplits == null) {
            translation = translations[0];
        } else {
            translation = TranslationJoiner.join(text, textSplits, translations);
        }
        translation.setDecodeTime(decodeTime);
        translation.setMemoryLookupTime(lookupTime);
        
        return translation;
    }

    @Override
    public void test() throws DecoderException {
        if (echoServer)
            return;

        if (decoderQueue.availability() < 1)
            throw new DecoderException("No decoder process available");

        long now = System.currentTimeMillis();
        if (now - lastSuccessfulTranslation < 5000L)
            return;

        synchronized (this) {
            if (now - lastSuccessfulTranslation < 5000L)
                return;

            PythonDecoder decoder = null;
            try {
                decoder = decoderQueue.poll(null, 100, TimeUnit.MILLISECONDS);

                // if timeout expired the system is busy translating,
                // we assume it is healthy for the moment.
                if (decoder != null) {
                    decoder.test();
                    lastSuccessfulTranslation = now;
                }
            } finally {
                if (decoder != null)
                    decoderQueue.release(decoder);
            }
        }
    }

    // DataListenerProvider

    @Override
    public Collection<DataListener> getDataListeners() {
        return Collections.singleton(memory);
    }

    // Closeable

    @Override
    public void close() {
        IOUtils.closeQuietly(this.decoderQueue);
        IOUtils.closeQuietly(this.memory);
    }

}
