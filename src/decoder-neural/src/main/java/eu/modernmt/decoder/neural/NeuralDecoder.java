package eu.modernmt.decoder.neural;

import eu.modernmt.config.DecoderConfig;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.DecoderListener;
import eu.modernmt.decoder.DecoderWithNBest;
import eu.modernmt.decoder.neural.execution.DecoderQueue;
import eu.modernmt.decoder.neural.execution.PythonDecoder;
import eu.modernmt.decoder.neural.execution.impl.DecoderQueueImpl;
import eu.modernmt.decoder.neural.execution.impl.PythonDecoderImpl;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.decoder.neural.memory.TranslationMemory;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

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
    private final Set<LanguagePair> directions;
    private final DecoderQueue decoderQueue;

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
            return DecoderQueueImpl.newGPUInstance(builder, decoderConfig.getGPUs());
        else
            return DecoderQueueImpl.newCPUInstance(builder, decoderConfig.getThreads());
    }

    // Decoder

    @Override
    public void setListener(DecoderListener listener) {
        decoderQueue.setListener(listener);
        listener.onTranslationDirectionsChanged(directions);
    }

    @Override
    public Translation translate(UUID user, LanguagePair direction, Sentence text) throws DecoderException {
        return translate(user, direction, text, null, 0);
    }

    @Override
    public Translation translate(UUID user, LanguagePair direction, Sentence text, int nbestListSize) throws DecoderException {
        return translate(user, direction, text, null, nbestListSize);
    }

    @Override
    public Translation translate(UUID user, LanguagePair direction, Sentence text, ContextVector contextVector) throws DecoderException {
        return translate(user, direction, text, contextVector, 0);
    }

    @Override
    public Translation translate(UUID user, LanguagePair direction, Sentence text, ContextVector contextVector, int nbestListSize) throws DecoderException {
        if (!this.directions.contains(direction))
            throw new UnsupportedLanguageException(direction);

        Translation translation;

        if (text.hasWords()) {
            ScoreEntry[] suggestions;

            try {
                suggestions = memory.search(user, direction, text, contextVector, this.suggestionsLimit);
            } catch (IOException e) {
                throw new DecoderException("Failed to retrieve suggestions from memory", e);
            }

            if (this.echoServer) {
                if (suggestions != null && suggestions.length > 0) {
                    translation = Translation.fromTokens(text, suggestions[0].translation);
                } else {
                    translation = Translation.fromTokens(text, TokensOutputStream.tokens(text, false, true));
                }
            } else {
                PythonDecoder decoder = null;

                try {
                    if (suggestions != null && suggestions.length > 0) {
                        // if perfect match, return suggestion instead
                        if (suggestions[0].score == 1.f) {
                            translation = Translation.fromTokens(text, suggestions[0].translation);
                        } else {
                            decoder = decoderQueue.take(direction);
                            translation = decoder.translate(direction, text, suggestions, nbestListSize);
                        }
                    } else {
                        decoder = decoderQueue.take(direction);
                        translation = decoder.translate(direction, text, nbestListSize);
                    }
                } finally {
                    if (decoder != null)
                        decoderQueue.release(decoder);
                }
            }

            if (logger.isDebugEnabled()) {
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
        } else {
            translation = Translation.emptyTranslation(text);
        }

        return translation;
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
