package eu.modernmt.decoder.neural;

import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderListener;
import eu.modernmt.decoder.neural.execution.ExecutionQueue;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.decoder.neural.memory.TranslationMemory;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.io.FileConst;
import eu.modernmt.lang.LanguageIndex;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Created by davide on 22/05/17.
 */
public class NeuralDecoder implements Decoder, DataListenerProvider {

    private static final Logger logger = LogManager.getLogger(NeuralDecoder.class);

    private final int suggestionsLimit;
    private final ExecutionQueue executor;
    private final TranslationMemory memory;
    private final Set<LanguagePair> directions;

    public NeuralDecoder(File modelPath, int[] gpus) throws NeuralDecoderException {
        ModelConfigFile config;
        try {
            config = ModelConfigFile.load(new File(modelPath, "model.conf"));
        } catch (IOException e) {
            throw new NeuralDecoderException("Failed to read file model.conf", e);
        }

        this.directions = config.getAvailableTranslationDirections();
        this.suggestionsLimit = config.getSuggestionsLimit();

        File pythonHome = new File(FileConst.getLibPath(), "pynmt");
        File storageModelPath = new File(modelPath, "memory");

        this.executor = ExecutionQueue.newInstance(pythonHome, modelPath, gpus);

        try {
            this.memory = new LuceneTranslationMemory(new LanguageIndex(this.directions), storageModelPath, config.getQueryMinimumResults());
        } catch (IOException e) {
            throw new NeuralDecoderException("Failed to initialize memory", e);
        }
    }

    // Decoder

    @Override
    public void setListener(DecoderListener listener) {
        listener.onTranslationDirectionsChanged(directions);
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence text) throws NeuralDecoderException {
        return translate(direction, text, null);
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence text, ContextVector contextVector) throws NeuralDecoderException {
        if (!this.directions.contains(direction))
            throw new UnsupportedLanguageException(direction);

        long start = System.currentTimeMillis();

        ScoreEntry[] suggestions;

        try {
            suggestions = memory.search(direction, text, contextVector, this.suggestionsLimit);
        } catch (IOException e) {
            throw new NeuralDecoderException("Failed to retrieve suggestions from memory", e);
        }

        Translation translation;

        if (suggestions != null && suggestions.length > 0)
            translation = executor.execute(direction, text, suggestions);
        else
            translation = executor.execute(direction, text);

        long elapsed = System.currentTimeMillis() - start;
        translation.setElapsedTime(elapsed);

        logger.info("Translation of " + text.length() + " words took " + (((double) elapsed) / 1000.) + "s");

        return translation;
    }

    // DataListenerProvider

    @Override
    public Collection<DataListener> getDataListeners() {
        return Collections.singleton(memory);
    }

    // Closeable

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(this.executor);
        IOUtils.closeQuietly(this.memory);
    }

}
