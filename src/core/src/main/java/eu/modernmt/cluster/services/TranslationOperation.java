package eu.modernmt.cluster.services;

import com.hazelcast.spi.Operation;
import eu.modernmt.cluster.TranslationTask;
import eu.modernmt.model.Translation;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * A TranslationOperation is an Hazelcast Operation for performing translations.
 * It basically contains a TranslationTask that this
 *
 * A cluster member can ask other members to perform TranslationOperations
 */
class TranslationOperation extends Operation {

    /**
     * A TranslationOperation.Result represents the result of a TranslationOperation.
     * It can be either successful (holding a not null Translation translation field)
     * or unsuccessful (holding a not null Throwable exception field).
     */
    public static class Result implements Serializable {    //to allow transmission throughout the cluster

        private final Translation translation;
        private final Throwable exception;

        public Result(Translation translation, Throwable exception) {
            this.translation = translation;
            this.exception = exception;
        }

        public Translation unwrap() throws ExecutionException {
            if (exception == null)
                return translation;

            throw new ExecutionException(exception);
        }
    }

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
            Result result;

            try {
                Translation translation = task.call();
                result = new Result(translation, null);
            } catch (Exception e) {
                result = new Result(null, e);
            }

            sendResponse(result);
        }
    }

    // ============================


    private final TranslationTask task;     // the translation task that this Operation must run

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
    public boolean returnsResponse() {
        return false;
    }

    @Override
    public String getResponse() {
        return null;
    }
}