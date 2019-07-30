package eu.modernmt.decoder.neural.scheduler;

import eu.modernmt.decoder.DecoderUnavailableException;

import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractScheduler<T extends Scheduler.Job> implements Scheduler {

    private final Queue<T> queue;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private boolean active = true;

    protected AbstractScheduler(Queue<T> queue) {
        this.queue = queue;
    }

    protected final void schedule(T job) throws DecoderUnavailableException {
        try {
            lock.lock();

            if (!active)
                throw new DecoderUnavailableException("Decoder has been shut down");

            int qSize = queue.size();
            if (queue.offer(job)) {
                job.onStartWaitingInQueue(qSize);
                notEmpty.signal();
            } else {
                throw new DecoderUnavailableException("Decoder unavailable due to a temporary overloading");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final Job take() throws InterruptedException {
        try {
            lock.lock();
            while (queue.isEmpty() && active)
                notEmpty.await();

            if (!queue.isEmpty())
                return queue.poll();

            // scheduler is not active anymore
            notEmpty.signal();  // pass the signal to next thread in queue
            throw new InterruptedException();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final void close() {
        try {
            lock.lock();
            active = false;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

}
