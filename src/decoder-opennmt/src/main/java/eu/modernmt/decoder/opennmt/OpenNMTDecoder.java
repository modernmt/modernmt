package eu.modernmt.decoder.opennmt;

import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.opennmt.execution.ExecutionQueue;
import eu.modernmt.decoder.opennmt.model.TranslationRequest;
import eu.modernmt.decoder.opennmt.storage.StorageException;
import eu.modernmt.decoder.opennmt.storage.Suggestion;
import eu.modernmt.decoder.opennmt.storage.SuggestionStorage;
import eu.modernmt.decoder.opennmt.storage.lucene.LuceneSuggestionStorage;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by davide on 22/05/17.
 */
public class OpenNMTDecoder implements Decoder, DataListenerProvider {

    private static final int SUGGESTIONS_LIMIT = 4;
    private final ExecutionQueue executor;
    private final SuggestionStorage storage;

    public OpenNMTDecoder(File libPath, File modelPath) throws OpenNMTException {
        File pythonHome = new File(libPath, "opennmt");
        File decoderModelPath = new File(modelPath, "model");
        File storageModelPath = new File(modelPath, "storage");

        ProcessBuilder builder = new ProcessBuilder("python", "nmt_decoder.py", decoderModelPath.getAbsolutePath());
        builder.directory(pythonHome);

        this.executor = ExecutionQueue.newSingleThreadExecutionQueue(builder);

        try {
            this.storage = new LuceneSuggestionStorage(storageModelPath);
        } catch (StorageException e) {
            throw new OpenNMTException("Failed to initialize storage", e);
        }
    }

    // Decoder

    @Override
    public Translation translate(Sentence text) throws OpenNMTException {
        return translate(text, null);
    }

    @Override
    public Translation translate(Sentence text, ContextVector contextVector) throws OpenNMTException {
        TranslationRequest request = new TranslationRequest(text);

        if (contextVector != null) {
            List<Suggestion> suggestions;
            try {
                suggestions = storage.getSuggestions(text, contextVector, SUGGESTIONS_LIMIT);
            } catch (StorageException e) {
                throw new OpenNMTException("Failed to retrieve suggestions from storage", e);
            }

            if (!suggestions.isEmpty())
                request.setSuggestions(suggestions);
        }

        return executor.execute(request).get();
    }

    // DataListenerProvider

    @Override
    public Collection<DataListener> getDataListeners() {
        return Collections.singleton(storage);
    }

    // Closeable

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(this.executor);
        IOUtils.closeQuietly(this.storage);
    }
}
