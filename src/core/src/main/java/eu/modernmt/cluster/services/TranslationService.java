package eu.modernmt.cluster.services;

import com.hazelcast.config.Config;
import com.hazelcast.config.ServiceConfig;
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
        TranslationServiceConfig config = new TranslationServiceConfig(properties);

        int threads = config.getThreads();
        int highPriorityQueueSize = config.getHighPriorityQueueSize();
        int normalPriorityQueueSize = config.getNormalPriorityQueueSize();
        int backgroundPriorityQueueSize = config.getBackgroundPriorityQueueSize();

        BlockingQueue<Runnable> queue = new PriorityBucketBlockingQueue<>(
                highPriorityQueueSize, normalPriorityQueueSize, backgroundPriorityQueueSize);

        this.nodeEngine = nodeEngine;
        this.executor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, queue) {

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

    public static TranslationServiceConfig getConfig(Config hazelcastConfig) {
        return new TranslationServiceConfig(hazelcastConfig
                .getServicesConfig()
                .getServiceConfig(SERVICE_NAME)
                .getProperties());

    }
}
