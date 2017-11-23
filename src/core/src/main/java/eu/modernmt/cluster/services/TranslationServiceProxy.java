
package eu.modernmt.cluster.services;

import com.hazelcast.nio.Address;
import com.hazelcast.spi.AbstractDistributedObject;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.OperationService;
import eu.modernmt.cluster.TranslationTask;
import eu.modernmt.model.Translation;

import java.util.concurrent.Future;

/**
 * A TranslationServiceProxy is an Hazelcast proxy for a TranslationService service.
 * @see TranslationService
 *
 * It is used by cluster members as a local endpoint to a remote TranslationService instance.
 *
 * A TranslationServiceProxy is typically spawned by a TranslationService instance in its own cluster node,
 * and keeps a reference to its TranslationService.
 */
public class TranslationServiceProxy extends AbstractDistributedObject<TranslationService> {

    private final String name;

    protected TranslationServiceProxy(NodeEngine nodeEngine, TranslationService service, String name) {
        super(nodeEngine, service);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getServiceName() {
        return TranslationService.SERVICE_NAME;
    }

    /**
     * This method allows this cluster Member to ask another Member to run a TranslationTask.
     * The TranslationServiceProxy will create a TranslationOperation for the task and
     * use the local OperationService to pass it to the TranslationService of the remote member.
     *
     * @param task the TranslationTask to run
     * @param address the Address of the Member that should run this task
     * @return a Future for the Translation that this task will output
     */
    public Future<Translation> submit(TranslationTask task, Address address) {
        OperationService localOperationService = getNodeEngine().getOperationService();
        TranslationOperation operation = new TranslationOperation(task);
        return localOperationService.invokeOnTarget(getServiceName(), operation, address);

        /* The resulting InternalCompletableFuture<TranslationOperation.Result>
        is wrapped in a Future<Translation> to allow easier handling by who called it
        (typically the ClusterNode)*/

        //return new FutureWrapper(future);
    }


    /**
     * A FutureWrapper is a private TranslationServiceProxy class
     * that allows transparent handling of TranslationOperation.Result instances.
     * @see TranslationOperation.Result
     */
//    private static class FutureWrapper implements Future<Translation> {
//
//        InternalCompletableFuture<Translation> future;
//
//        public FutureWrapper(InternalCompletableFuture<Translation> future) {
//            this.future = future;
//        }
//
//        @Override
//        public boolean cancel(boolean mayInterruptIfRunning) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public boolean isCancelled() {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public boolean isDone() {
//            throw new UnsupportedOperationException();
//        }
//
//        /* When a TranslationOperation is run, it returns an InternalCompletableFuture<TranslationOperation.Result> object.
//        * The TranslationOperation.Result object is necessary because the translation task
//        * can be either successful (a translation) or unsuccessful (an exception),
//        * but it is not good that the
//        *
//        * A FutureWrapper is just a Future for Translation objects that unwraps the TranslationOperation.Result object returning the corresponding translation.
//        * Note that during this unwrap, if the translation was unsuccessful, the result exception is thrown.
//*/
//
//        @Override
//        public Translation get() throws InterruptedException, ExecutionException {
//            return future.get().unwrap();
//        }
//
//        @Override
//        public Translation get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
//            return future.get(timeout, unit).unwrap();
//        }
//
//    }
}