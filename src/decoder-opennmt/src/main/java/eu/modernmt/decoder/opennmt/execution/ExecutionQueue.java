package eu.modernmt.decoder.opennmt.execution;

import eu.modernmt.decoder.opennmt.OpenNMTException;
import eu.modernmt.decoder.opennmt.model.TranslationRequest;
import eu.modernmt.model.Translation;

import java.io.Closeable;

/**
 * Created by davide on 22/05/17.
 */
public interface ExecutionQueue extends Closeable {

    static ExecutionQueue newSingleThreadExecutionQueue(ProcessBuilder builder) throws OpenNMTException {
        return new SingleThreadExecutionQueue(builder);
    }

    interface PendingTranslation {

        Translation get() throws OpenNMTException;

    }

    PendingTranslation execute(TranslationRequest request) throws OpenNMTException;

}
