package eu.modernmt.cluster.services;

import com.hazelcast.spi.Operation;

import java.util.concurrent.Callable;

/**
 * An IPriorityCallableOperation is an Operation that executes an ICallable.
 * It is typically used when a cluster member requests another cluster member to run a specific task.
 *
 * The ICallable is executed asynchronously, meaning that the IPriorityCallableOperation ends
 * (and thus frees the corresponding operationThread)
 * *before* the end of the callable execution.
 *
 * The callable will keep a reference to the IPriorityCallableOperation,
 * so it will be able to send the response to the original requester through the sendResponse method.
 * @param <T>
 */
public class IPriorityCallableOperation<T> extends Operation {

    private final Callable<T> callable;
    private final IPriorityExecutorService.Priority priority;

    public IPriorityCallableOperation(Callable<T> callable, IPriorityExecutorService.Priority priority) {
        this.callable = callable;
        this.priority = priority;
    }
    @Override
    public void run() throws Exception {
        ICallable.CallableWrapper ICallable = ICallable.w
        /*get executor*/
        /*submit callable*/
        /* -- ends before the end of the callable! -- */
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
