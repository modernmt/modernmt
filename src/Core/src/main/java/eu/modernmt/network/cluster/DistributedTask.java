package eu.modernmt.network.cluster;

import eu.modernmt.network.uuid.UUIDSequence;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Created by davide on 20/11/15.
 */
public class DistributedTask<V extends Serializable> implements RunnableFuture<V> {

    private static final int ANY = -1;
    private static final int NEW = 0;
    private static final int COMPLETING = 1;
    private static final int NORMAL = 2;
    private static final int EXCEPTIONAL = 3;
    private static final int CANCELLED = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED = 6;

    private UUID id;

    private Cluster cluster;
    private int state;

    private DistributedCallable<V> callable;
    private Object outcome;
    private CountDownLatch completionCountDown;

    DistributedTask() {

    }

    public DistributedTask(Cluster cluster, DistributedCallable<V> callable) {
        this.id = UUIDSequence.next(UUIDSequence.SequenceType.DISTRIBUTED_TASK);
        this.cluster = cluster;
        this.callable = callable;
        this.state = NEW;
        this.completionCountDown = new CountDownLatch(1);
    }

    DistributedCallable<V> getCallable() {
        return this.callable;
    }

    public UUID getId() {
        return this.id;
    }

    private synchronized boolean compareAndSwapState(int expected, int state) {
        if (this.state == expected || expected == ANY) {
            this.state = state;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void run() {
        if (state != NEW)
            return;

        this.cluster.exec(this);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!(state == NEW && compareAndSwapState(NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            return false;

        try {
            this.cluster.removeFromExecution(this, mayInterruptIfRunning);
        } finally {
            this.compareAndSwapState(ANY, INTERRUPTED);
            onTaskComplete();
        }

        return true;
    }

    @Override
    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state != NEW;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        int s = state;

        if (s <= COMPLETING)
            this.completionCountDown.await();

        return report();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        int s = state;

        if (s <= COMPLETING) {
            this.completionCountDown.await(timeout, unit);
            s = state;

            if (s <= COMPLETING)
                throw new TimeoutException();
        }

        return report();
    }

    protected void done() {

    }

    void set(Object v) {
        if (compareAndSwapState(NEW, COMPLETING)) {
            outcome = v;
            compareAndSwapState(ANY, NORMAL);
            onTaskComplete();
        }
    }

    void setException(Throwable t) {
        if (compareAndSwapState(NEW, COMPLETING)) {
            outcome = t;
            compareAndSwapState(ANY, EXCEPTIONAL);
            onTaskComplete();
        }
    }

    private void onTaskComplete() {
        this.completionCountDown.countDown();
        this.done();
        this.callable = null;
    }

    @SuppressWarnings("unchecked")
    private V report() throws ExecutionException {
        int s = state;
        Object x = outcome;

        if (s == NORMAL)
            return (V) x;
        if (s >= CANCELLED)
            throw new CancellationException();

        throw new ExecutionException((Throwable) x);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DistributedTask<?> that = (DistributedTask<?>) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
