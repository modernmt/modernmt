package eu.modernmt;

import java.util.Comparator;
import java.util.concurrent.*;

/**
 * A PriorityThreadPoolExecutor is a ThreadPoolExecutor that executes in an order based on their priority.
 */
public class PriorityThreadPoolExecutor extends ThreadPoolExecutor{

    public PriorityThreadPoolExecutor(int queueSize, int threads, long keepAlive, TimeUnit timeUnit) {
        super(threads, threads, keepAlive, timeUnit, new BoundedPriorityBlockingQueue<>(
                        queueSize, Comparator.comparingInt(o -> ((PriorityFutureTask) o).getPriority())));
    }

    protected <T> PriorityFutureTask<T> newTaskFor(Callable<T> callable) {
        int priority = callable instanceof PriorityCallable ? ((PriorityCallable) callable).priority : Priority.NORMAL;
        return new PriorityFutureTask<>(callable, priority);
    }

    /**
     * This private class is a blocking queue with priority policy and with an upper bound in its size.
     */
    private static class BoundedPriorityBlockingQueue<T> extends PriorityBlockingQueue<T> {

        /* Basically, it extends the PriorityBlockingQueue and before any insert  */
        private final int maxSize;

        public BoundedPriorityBlockingQueue(int size, Comparator<T> comparator) {
            super(size, comparator);
            this.maxSize = size;
        }

        @Override
        public boolean offer(T newElement) {
            return this.size() < this.maxSize && super.offer(newElement);
        }

        /* I did these implementations for completeness, but they should not be used
         * because the ThreadPoolExecutor only calls offer(T e) */

        @Override
        public boolean offer(T newElement, long timeout, TimeUnit unit) {
            return this.size() < this.maxSize && super.offer(newElement, timeout, unit);
        }

        @Override
        public boolean add(T newElement) {
            return this.size() < this.maxSize && super.add(newElement);
        }
    }
}
