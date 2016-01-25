package eu.modernmt.network.cluster;

import java.util.ArrayList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;

/**
 * Created by davide on 20/11/15.
 */
public class ExecutionQueue {

    private ConcurrentHashMap<UUID, DistributedTask<?>> running = new ConcurrentHashMap<>();
    private Queue<DistributedTask<?>> queue = new ConcurrentLinkedQueue<>();
    private boolean shutdown = false;

    /**
     * Retrieves the next element in queue and moves it to running queue.
     *
     * @return the next element in queue, or null if the queue is empty or has been shut down
     */
    public DistributedTask<?> next() {
        if (shutdown)
            return null;

        DistributedTask<?> task = queue.poll();

        if (task == null)
            return null;

        boolean cancelTask;

        synchronized (this) {
            if (shutdown) {
                cancelTask = true;
            } else {
                cancelTask = false;
                this.running.put(task.getId(), task);
            }
        }

        if (cancelTask) {
            task.cancel(true);
            return null;
        } else {
            return task;
        }
    }

    public int size() {
        return this.queue.size();
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public synchronized void add(DistributedTask<?> task) {
        if (this.shutdown)
            throw new RejectedExecutionException();

        this.queue.add(task);
    }

    public void shutdown() {
        if (!this.shutdown) {
            synchronized (this) {
                if (!this.shutdown) {
                    this.shutdown = true;
                } else {
                    return;
                }
            }
        }

        ArrayList<DistributedTask<?>> pending = new ArrayList<>(this.queue.size() + this.running.size());
        pending.addAll(this.queue);
        pending.addAll(this.running.values());

        this.queue.clear();
        this.running.clear();

        for (DistributedTask<?> task : pending) {
            task.cancel(true);
        }
    }

    void remove(DistributedTask<?> task, boolean removeFromRunningQueue) {
        queue.remove(task);

        if (removeFromRunningQueue)
            this.removeRunningTask(task.getId());
    }

    DistributedTask<?> removeRunningTask(UUID id) {
        return this.running.remove(id);
    }

}
