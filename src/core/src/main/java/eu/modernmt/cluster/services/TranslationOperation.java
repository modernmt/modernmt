package eu.modernmt.cluster.services;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.impl.operationservice.impl.responses.ErrorResponse;
import com.hazelcast.spi.impl.operationservice.impl.responses.NormalResponse;
import eu.modernmt.cluster.TranslationTask;
import eu.modernmt.model.Translation;
import org.apache.commons.lang.SerializationUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.concurrent.ExecutorService;

/**
 * A TranslationOperation is an Hazelcast Operation for performing translations.
 * It basically contains a TranslationTask that this
 *
 * A cluster member can ask other members to perform TranslationOperations
 */
class TranslationOperation extends Operation {

    /**
     * A TranslationOperation.ComparableRunnable is a Runnable built specifically to contain and handle a TranslationTask.
     * More specifically, when it is run, it executes the TranslationTask in the same thread
     * and sends the corresponding response to the requesting cluster member
     * using the TranslationOperation.sendResponse method.
     *
     * Note that this implies using Operations *asynchronously*, as when the sendResponse method is called
     * the TranslationOperation.run() execution itself has already ended a while ago.
     */
    public class ComparableRunnable implements Runnable, Comparable<ComparableRunnable> {

        private final TranslationTask task;

        public ComparableRunnable(TranslationTask task) {
            this.task = task;
        }

        @Override
        public int compareTo(@NotNull ComparableRunnable o) {
            return task.compareTo(o.task);
        }

        @Override
        public void run() {
            try {
                Translation translation = task.call();
                sendResponse(new NormalResponse(translation, getCallId(), 0, false));
            } catch (Throwable e) {
                sendResponse(new ErrorResponse(e, getCallId(), false));
            }
        }
    }

    // ============================


    private TranslationTask task;     // the translation task that this Operation must run

    // necessary for deserialization
    public TranslationOperation() {
    }

    public TranslationOperation(TranslationTask translationCallable) {
        this.task = translationCallable;
    }

    @Override
    public void run() throws Exception {
        TranslationService translationService = getService();
        ExecutorService executor = translationService.getExecutor();

        executor.submit(new ComparableRunnable(task));
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        byte[] taskBytes = SerializationUtils.serialize(this.task);
        out.writeByteArray(taskBytes);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        byte[] taskBytes = in.readByteArray();
        this.task = (TranslationTask) SerializationUtils.deserialize(taskBytes);
    }
    
    @Override
    public boolean returnsResponse() {
        return false;
    }

    @Override
    public String getResponse() {
        return null;
    }


}