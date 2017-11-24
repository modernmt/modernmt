package eu.modernmt.cluster.services;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A PriorityThreadPoolExecutor is a ThreadPoolExecutor that executes tasks in an order based on their priority.
 */
public class PriorityThreadPoolExecutor extends ThreadPoolExecutor {

    public PriorityThreadPoolExecutor(int threads, long keepAlive, TimeUnit timeUnit, int... queueSizes) {

        /* create a TranslationRunnable BoundedPriorityBlockingQueue and pass it to the parent ThreadPoolExecutor
        together with core threads number, max threads number, keepalive and timeunit) */
        super(threads, threads, keepAlive, timeUnit, new PriorityBucketBlockingQueue<>(queueSizes));
    }
}
