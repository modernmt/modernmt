package eu.modernmt.network.cluster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by davide on 20/11/15.
 */
public class ExecutionQueue {

    private ConcurrentHashMap<UUID, DistributedTask<?>> pending = new ConcurrentHashMap<>();
    private Queue<DistributedTask<?>> queue = new ConcurrentLinkedQueue<>();
    private boolean shutdown = false;

    private Lock terminationLock = new ReentrantLock();
    private CountDownLatch terminationCountDown;

    /**
     * Retrieves the next element in queue and moves it to pending queue.
     *
     * @return the next element in queue, or null if the queue is empty or has been shut down
     */
    public DistributedTask<?> next() {
        if (this.shutdown)
            return null;

        DistributedTask<?> task = queue.poll();

        if (task == null)
            return null;

        if (!this.shutdown) {
            synchronized (this) {
                if (!this.shutdown) {
                    this.pending.put(task.getId(), task);
                    return task;
                }
            }
        }

        return null;
    }

    public int size() {
        return this.queue.size();
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public boolean isTerminated() {
        return terminationCountDown != null && terminationCountDown.getCount() == 0;
    }

    public void awaitTermination() throws InterruptedException {
        if (terminationCountDown != null)
            terminationCountDown.await();
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (terminationCountDown != null)
            terminationCountDown.await(timeout, unit);
    }

    public synchronized void add(DistributedTask<?> task) {
        if (this.shutdown)
            throw new RejectedExecutionException();

        this.queue.add(task);
    }

    public synchronized List<DistributedTask<?>> shutdown(boolean removeFromPending) {
        ArrayList<DistributedTask<?>> pending = null;

        this.shutdown = true;
        this.queue.clear();

        if (removeFromPending) {
            pending = new ArrayList<>(this.pending.size());
            pending.addAll(this.pending.values());
            this.pending.clear();
        }

        terminationLock.lock();

        try {
            terminationCountDown = new CountDownLatch(this.pending.size());
        } finally {
            terminationLock.unlock();
        }

        return pending;
    }

    public void remove(DistributedTask<?> task, boolean removeFromPendingQueue) {
        queue.remove(task);

        if (removeFromPendingQueue) {
            /*
             * We accept the risk that:
             *   1 • next() take the task from the queue
             *   2 • remove() completes the execution
             *   3 • next() add the task to the pending map
             *
             * This is a safe condition because the task has been marked
             * as cancelled and its result won't be dispatched anymore.
             */

            pending.remove(task.getId());
        }
    }

    public DistributedTask<?> removePendingTask(UUID id) {
        DistributedTask<?> task;

        terminationLock.lock();
        try {
            task = this.pending.remove(id);
            if (task != null && terminationCountDown != null)
                terminationCountDown.countDown();
        } finally {
            terminationLock.unlock();
        }

        return task;
    }

}
