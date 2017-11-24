package eu.modernmt.cluster.services;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.spi.ManagedService;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.RemoteService;

import java.util.Properties;
import java.util.concurrent.*;

/**
 * A TranslationService is an Hazelcast Service for performing translations in a ModernMT cluster.
 * It allows cluster members  to ask to other cluster members to perform translations;
 * translations, moreover, will be run taking their priority into account.
 * <p>
 * As always in Hazelcast Services, this service is reached by cluster members through local proxies.
 *
 * @see TranslationServiceProxy
 * <p>
 * This TranslationService is typically initialized at cluster start.
 */
public class TranslationService implements ManagedService, RemoteService {

    public static final String SERVICE_NAME = "mmt:cluster:TranslationService";

    private NodeEngine nodeEngine;
    private ExecutorService executor;

    @Override
    public void init(NodeEngine nodeEngine, Properties properties) {
        this.nodeEngine = nodeEngine;


        // TODO: get these values from properties
        int background_priority_queue_size = 10;
        int normal_priority_queue_size = 100000;
        int high_priority_queue_size = 100;

        int threads = Integer.parseInt(properties.getProperty("parallelism-degree"));

        this.executor = new ThreadPoolExecutor(threads, threads,
                0L, TimeUnit.MILLISECONDS,
                new PriorityBucketBlockingQueue<>(high_priority_queue_size, normal_priority_queue_size, background_priority_queue_size)) {

            @Override
            protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
                RunnableFuture<T> task = super.newTaskFor(runnable, value);

                if (runnable instanceof Prioritizable)
                    task = new PriorityFuture<>(task, ((Prioritizable) runnable).getPriority());

                return task;
            }

            @Override
            protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                RunnableFuture<T> task = super.newTaskFor(callable);

                if (callable instanceof Prioritizable)
                    task = new PriorityFuture<>(task, ((Prioritizable) callable).getPriority());

                return task;
            }
        };
    }

    ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public void reset() {

    }

    @Override
    public void shutdown(boolean terminate) {
        executor.shutdownNow();
    }

    @Override
    public DistributedObject createDistributedObject(String objectName) {
        return new TranslationServiceProxy(nodeEngine, this, objectName);
    }

    @Override
    public void destroyDistributedObject(String objectName) {

    }
}
