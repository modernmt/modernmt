package eu.modernmt.decoder.opennmt;

import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.opennmt.execution.ExecutionQueue;
import eu.modernmt.decoder.opennmt.model.TranslationRequest;
import eu.modernmt.decoder.opennmt.storage.StorageException;
import eu.modernmt.decoder.opennmt.storage.TranslationsStorage;
import eu.modernmt.decoder.opennmt.storage.lucene.LuceneTranslationsStorage;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by davide on 22/05/17.
 */
public class OpenNMTDecoder implements Decoder, DataListenerProvider {

    private static final int SUGGESTIONS_LIMIT = 4;
    private final ExecutionQueue executor;
    private final TranslationsStorage storage;

    public OpenNMTDecoder(File libPath, File modelPath) throws OpenNMTException {
        File pythonHome = new File(libPath, "opennmt");
        File decoderModelPath = new File(modelPath, "model");
        File storageModelPath = new File(modelPath, "storage");

        ProcessBuilder builder = new ProcessBuilder("python", "nmt_decoder.py", decoderModelPath.getAbsolutePath());
        builder.directory(pythonHome);

        this.executor = ExecutionQueue.newSingleThreadExecutionQueue(builder);

        try {
            this.storage = new LuceneTranslationsStorage(storageModelPath);
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
            TranslationsStorage.SearchResult result;

            try {
                result = storage.search(text, contextVector, SUGGESTIONS_LIMIT);
            } catch (StorageException e) {
                throw new OpenNMTException("Failed to retrieve suggestions from storage", e);
            }

            if (!result.isEmpty())
                request.setSuggestions(result);
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
