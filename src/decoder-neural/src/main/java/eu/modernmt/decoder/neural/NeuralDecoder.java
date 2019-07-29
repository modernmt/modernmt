package eu.modernmt.decoder.neural;

import eu.modernmt.config.DecoderConfig;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderListener;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.decoder.neural.queue.*;
import eu.modernmt.decoder.neural.scheduler.Scheduler;
import eu.modernmt.decoder.neural.scheduler.SentenceBatchScheduler;
import eu.modernmt.decoder.neural.scheduler.TranslationSplit;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.memory.TranslationMemory;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Priority;
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
public class NeuralDecoder extends Decoder implements DataListenerProvider {

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
    private final Scheduler scheduler;
    private final DecoderExecutor[] executors;
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
        this.decoderQueue = this.echoServer ? new EchoServerDecoderQueue() : loadDecoderQueue(modelConfig, config, model);

        // Scheduler
        this.scheduler = createScheduler(modelConfig, config.getQueueSize());

        // Executors
        this.executors = new DecoderExecutor[this.decoderQueue.size()];
        for (int i = 0; i < this.executors.length; i++) {
            this.executors[i] = new DecoderExecutor(this.scheduler, this.decoderQueue);
            this.executors[i].start();
        }
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

    protected Scheduler createScheduler(ModelConfig modelConfig, int queueSize) throws DecoderException {
        return new SentenceBatchScheduler(queueSize);
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
    public boolean isLanguageSupported(LanguageDirection language) {
        return this.directions.contains(language);
    }

    @Override
    public Translation translate(Priority priority, UUID user, LanguageDirection direction, Sentence text, long timeout) throws DecoderException {
        return translate(priority, user, direction, text, null, timeout);
    }

    @Override
    public Translation translate(Priority priority, UUID user, LanguageDirection direction, Sentence text, ContextVector context, long timeout) throws DecoderException {
        if (!isLanguageSupported(direction))
            throw new UnsupportedLanguageException(direction);

        if (!text.hasWords())
            return Translation.emptyTranslation(text);

        // Search for suggestions
        long lookupBegin = System.currentTimeMillis();
        ScoreEntry[] suggestions = lookup(user, direction, text, context);
        long lookupTime = System.currentTimeMillis() - lookupBegin;

        // Scheduling translation
        Scheduler.TranslationLock lock;
        TranslationSplit[] splits;

        if (suggestions != null && suggestions[0].score == 1.f) {  // align
            TranslationSplit split = new TranslationSplit(priority, text, suggestions[0].translation, timeout);
            splits = new TranslationSplit[]{split};
            lock = scheduler.schedule(direction, split);
        } else {
            List<Sentence> textSplits = split(text);
            splits = new TranslationSplit[textSplits.size()];

            int i = 0;
            for (Sentence textSplit : textSplits)
                splits[i++] = new TranslationSplit(priority, textSplit, timeout);

            lock = scheduler.schedule(direction, splits, suggestions);
        }

        // Wait for translation to be completed
        try {
            lock.await();

            Translation translation = TranslationJoiner.join(text, splits);
            translation.setMemoryLookupTime(lookupTime);

            if (logger.isDebugEnabled()) {
                String sourceText = TokensOutputStream.serialize(text, false, true);
                String targetText = TokensOutputStream.serialize(translation, false, true);

                StringBuilder log = new StringBuilder("Translation received from neural decoder:\n" +
                        "   sentence = " + sourceText + "\n" +
                        "   translation = " + targetText + "\n" +
                        "   suggestions = [\n");

                if (suggestions != null) {
                    for (ScoreEntry entry : suggestions)
                        log.append("      ").append(entry).append('\n');
                }

                log.append("   ]");

                logger.debug(log);
            }

            return translation;
        } catch (InterruptedException e) {
            throw new DecoderException("Decoder interrupted", e);
        }
    }

    protected List<Sentence> split(Sentence sentence) {
        return SentenceSplitter.split(sentence);
    }

    protected ScoreEntry[] lookup(UUID user, LanguageDirection direction, Sentence text, ContextVector contextVector) throws DecoderException {
        ScoreEntry[] entries = null;

        if (text.hasWords() && contextVector != null && !contextVector.isEmpty()) {
            try {
                entries = memory.search(user, direction, text, contextVector, suggestionsLimit);
            } catch (IOException e) {
                throw new DecoderException("Failed to retrieve suggestions from memory", e);
            }
        }

        return entries != null && entries.length > 0 ? entries : null;
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
        IOUtils.closeQuietly(this.scheduler);

        for (Thread executor : executors) {
            try {
                executor.join();
            } catch (InterruptedException e) {
                // Ignore it
            }
        }

        IOUtils.closeQuietly(this.decoderQueue);
        IOUtils.closeQuietly(this.memory);
    }

}
