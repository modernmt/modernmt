package eu.modernmt.cluster.services;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by davide on 24/11/17.
 * A PriorityRunnableFuture is a Prioritizable wrapper for a Runnable Future.
 */
class PriorityRunnableFuture<T> implements RunnableFuture<T>, Prioritizable {

    private RunnableFuture<T> src;
    private int priority;

    public PriorityRunnableFuture(RunnableFuture<T> other, int priority) {
        this.src = other;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return src.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return src.isCancelled();
    }

    public boolean isDone() {
        return src.isDone();
    }

    public T get() throws InterruptedException, ExecutionException {
        return src.get();
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return src.get(timeout, unit);
    }

    public void run() {
        src.run();
    }

}
