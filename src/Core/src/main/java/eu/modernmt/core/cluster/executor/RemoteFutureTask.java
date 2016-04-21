package eu.modernmt.core.cluster.executor;

import java.util.concurrent.FutureTask;

/**
 * Created by davide on 20/04/16.
 */
class RemoteFutureTask<V> extends FutureTask<V> {

    private final DistributedExecutor executor;
    private final Task<V> task;

    public RemoteFutureTask(Task<V> task, DistributedExecutor executor) {
        super(task.callable);
        this.task = task;
        this.executor = executor;
    }

    public Task<V> getTask() {
        return task;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean result = super.cancel(mayInterruptIfRunning);

        if (result)
            executor.cancel(this);

        return result;
    }

    @Override
    protected void set(V v) {
        super.set(v);
    }

    @Override
    protected void setException(Throwable t) {
        super.setException(t);
    }
}
