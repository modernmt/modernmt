package eu.modernmt.cluster.services;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.spi.ManagedService;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.RemoteService;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A TranslationService is an Hazelcast Service for performing translations in a ModernMT cluster.
 * It allows cluster members  to ask to other cluster members to perform translations;
 * translations, moreover, will be run taking their priority into account.
 *
 * As always in Hazelcast Services, this service is reached by cluster members through local proxies.
 * @see TranslationServiceProxy
 *
 * This TranslationService is typically initialized at cluster start.
 */
public class TranslationService  implements ManagedService, RemoteService {

    public static final String SERVICE_NAME = "mmt:cluster:TranslationService";

    private NodeEngine nodeEngine;
    private ExecutorService executor;

    @Override
    public void init(NodeEngine nodeEngine, Properties properties) {
        this.nodeEngine = nodeEngine;


        // TODO: get these values from properties
        int POOL_SIZE = 10;
        int QUEUE_SIZE = 100;
        long KEEP_ALIVE = 20L;
        TimeUnit TIME_UNIT = TimeUnit.SECONDS;
        this.executor = new PriorityThreadPoolExecutor(QUEUE_SIZE, POOL_SIZE, KEEP_ALIVE, TIME_UNIT);
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
