package eu.modernmt.decoder.neural;

import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderListener;
import eu.modernmt.decoder.DecoderWithNBest;
import eu.modernmt.decoder.neural.execution.ExecutionQueue;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.decoder.neural.memory.TranslationMemory;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.io.FileConst;
import eu.modernmt.io.TokensOutputStream;
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
public class NeuralDecoder implements Decoder, DecoderWithNBest, DataListenerProvider {

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
        return translate(direction, text, null, 0);
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence text, int nbestListSize) throws NeuralDecoderException {
        return translate(direction, text, null, nbestListSize);
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence text, ContextVector contextVector) throws NeuralDecoderException {
        return translate(direction, text, contextVector, 0);
    }

    @Override
    public Translation translate(LanguagePair direction, Sentence text, ContextVector contextVector, int nbestListSize) throws NeuralDecoderException {
        if (!this.directions.contains(direction))
            throw new UnsupportedLanguageException(direction);

        long start = System.currentTimeMillis();


        Translation translation;

        if (text.hasWords()) {
            ScoreEntry[] suggestions;

            try {
                suggestions = memory.search(direction, text, contextVector, this.suggestionsLimit);
            } catch (IOException e) {
                throw new NeuralDecoderException("Failed to retrieve suggestions from memory", e);
            }

            if (suggestions != null && suggestions.length > 0)
                translation = executor.execute(direction, text, suggestions, nbestListSize);
            else
                translation = executor.execute(direction, text, nbestListSize);

            if (logger.isTraceEnabled()) {
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

                logger.trace(log);
            }
        } else {
            translation = Translation.emptyTranslation(text);
        }

        long elapsed = System.currentTimeMillis() - start;
        translation.setElapsedTime(elapsed);

        if (logger.isDebugEnabled())
            logger.debug("Translation of " + text.length() + " words took " + (((double) elapsed) / 1000.) + "s");

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

    @Override
    public boolean supportsSentenceSplit() {
        return true;
    }
}
