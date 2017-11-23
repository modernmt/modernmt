package eu.modernmt.cluster.services;

import com.hazelcast.spi.Operation;

import java.util.concurrent.Callable;

/**
 * An ICallable is a Callable designed to work in a HazelcastCluster in an async fashion.
 * It keeps a reference to an Operation (typically, the IPriorityCallableOperation it belongs to).
 * At the end of its call() method execution, it always runs the operation sendResponse() method
 * to asynchronously send the result back.
 * @param <T>
 */
public abstract class ICallable<T> implements Callable<T> {
    protected final Operation operation;

    public ICallable(Operation operation) {
        super();
        this.operation = operation;     //the operation that it belongs to
    }

    /**
     * The call method of an ICallable executes the callable task, gets its response
     * and sends it as the answer of the Operation of the ICallable itself
     * @return the result of the task execution
     */
    @Override
    public T call() throws Exception {
        T result = this.runCallable();
        this.operation.sendResponse(result);
        return result;  //if the operation is a IPriorityCallableOperation it will not use this, though
    }


    public static <T> ICallable<T> wrap(Callable<T> callable, Operation operation) {

        return new ICallable<T>(operation, callable) {
            private final Callable<T> callable;

            public ICallable(Operation operation, Callable<T> callable) {
                super(operation);
                this.callable = callable;
            }

            @Override
            public T runCallable() throws Exception {
                return this.callable.call();
            }

        }

    }

    /**
     * The runCallable method of an ICallable executes the "true" task of this ICallable
     *
     * @return the result of the task execution
     */
    public abstract T runCallable() throws Exception;

    /**
     * A CallableWrapper wraps a Callable into an ICallable
     * */
    public static class CallableWrapper<T> extends ICallable<T> {
        private final Callable<T> callable;

        public CallableWrapper(Operation operation, Callable<T> callable) {
            super(operation);
            this.callable = callable;
        }

        @Override
        public T runCallable() throws Exception {
            return this.callable.call();
        }
    }
}
