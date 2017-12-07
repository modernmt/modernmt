package eu.modernmt.cluster.services;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.impl.operationservice.impl.OperationServiceImpl;
import com.hazelcast.spi.impl.operationservice.impl.responses.ErrorResponse;
import com.hazelcast.spi.impl.operationservice.impl.responses.NormalResponse;
import eu.modernmt.cluster.TranslationTask;
import eu.modernmt.model.Translation;
import org.apache.commons.lang.SerializationUtils;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * A TranslationOperation is an Hazelcast Operation for performing translations.
 * It basically contains a TranslationTask that this
 * <p>
 * A cluster member can ask other members to perform TranslationOperations
 */
class TranslationOperation extends Operation {

    /**
     * A TranslationOperation.TranslationRunnable is a Runnable built specifically to contain and handle a TranslationTask.
     * More specifically, when it is run, it executes the TranslationTask in the same thread
     * and sends the corresponding response to the requesting cluster member
     * using the TranslationOperation.sendResponse method.
     * <p>
     * Note that this implies using Operations *asynchronously*, as when the sendResponse method is called
     * the TranslationOperation.run() execution itself has already ended a while ago.
     */
    public class TranslationRunnable implements Runnable, Prioritizable {

        private final TranslationTask task;

        public TranslationRunnable(TranslationTask task) {
            this.task = task;
        }

        @Override
        public void run() {
            try {
                startAsyncOperation();
                Translation translation = task.call();
                sendResponse(new NormalResponse(translation, getCallId(), 0, false));
            } catch (Throwable e) {
                sendResponse(new ErrorResponse(e, getCallId(), false));
            } finally {
                completeAsyncOperation();
            }
        }

        @Override
        public int getPriority() {
            return task.getPriority();
        }
    }


    private TranslationTask task;
    private transient Throwable submitException;

    // necessary for deserialization
    @SuppressWarnings("unused")
    public TranslationOperation() {
    }

    public TranslationOperation(TranslationTask translationCallable) {
        this.task = translationCallable;
    }

    @Override
    public void run() throws Exception {
        TranslationService translationService = getService();
        ExecutorService executor = translationService.getExecutor();

        try {
            executor.submit(new TranslationRunnable(task));
        } catch (Throwable e) {
            submitException = e;
        }
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
        return submitException != null;
    }

    @Override
    public Object getResponse() {
        return submitException == null ? null : new ErrorResponse(submitException, getCallId(), false);
    }

    /**
     * This method registers that an operation is still alive
     * but it is being processed on an another thread not belonging to the Operation thread pool.
     * <p>
     * This way, the operation will be considered as still running and Hazelcast will use its own structures to check its health.
     * <p>
     * Without this, the operation heartbeats are not sent
     * (since the operation service does not know that the response is yet to be sent)
     * and if enough operation heartbeats are not sent, the caller may declare the operation timed out.
     */
    public void startAsyncOperation() {
        /*get a reference to the local OperationService proxy*/
        OperationServiceImpl operationServiceProxy = (OperationServiceImpl) this.getNodeEngine().getOperationService();
        operationServiceProxy.onStartAsyncOperation(this);
    }

    /**
     * This method registers that an operation that has been offloaded to another thread,
     * and that was previously marked as alive with the startAsyncOperation method, is now completed.
     * <p>
     * This method must be called both if the task was successful and if it was not successful.
     * <p>
     * It must be called when the TranslationTask that this Operation is wrapping is completely over.
     */
    public void completeAsyncOperation() {
        /*get a reference to the local OperationService proxy*/
        OperationServiceImpl operationService = (OperationServiceImpl) this.getNodeEngine().getOperationService();
        operationService.onCompletionAsyncOperation(this);
    }

}