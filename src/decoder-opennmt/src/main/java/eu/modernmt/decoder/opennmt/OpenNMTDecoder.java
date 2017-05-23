package eu.modernmt.decoder.opennmt;

import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.opennmt.execution.ExecutionQueue;
import eu.modernmt.decoder.opennmt.model.TranslationRequest;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Translation;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 22/05/17.
 */
public class OpenNMTDecoder implements Decoder {

    private final ExecutionQueue executor;

    public OpenNMTDecoder(File libPath, File modelPath) throws OpenNMTException {
        File pythonHome = new File(libPath, "opennmt");

        ProcessBuilder builder = new ProcessBuilder("python", "nmt_decoder.py", modelPath.getAbsolutePath());
        builder.directory(pythonHome);

        this.executor = ExecutionQueue.newSingleThreadExecutionQueue(builder);
    }

    // Decoder

    @Override
    public Translation translate(Sentence text) throws OpenNMTException {
        return translate(text, null);
    }

    @Override
    public Translation translate(Sentence text, ContextVector contextVector) throws OpenNMTException {
        TranslationRequest request = new TranslationRequest(text);
        return executor.execute(request).get();
    }

    // Closeable

    @Override
    public void close() throws IOException {
        this.executor.close();
    }

}
