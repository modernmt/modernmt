package eu.modernmt.cluster.services;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.spi.ManagedService;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.RemoteService;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        this.executor = Executors.newCachedThreadPool();
    }

    ExecutorService getExecutor() {
        return executor;
    }

    @Override
    public void reset() {
        // nothing to do
    }

    @Override
    public void shutdown(boolean terminate) {
        executor.shutdownNow();
    }

    /**
     * This method returns a local proxy to the TranslationService.
     */
    @Override
    public DistributedObject createDistributedObject(String objectName) {
        return new TranslationServiceProxy(nodeEngine, this, objectName);
    }

    @Override
    public void destroyDistributedObject(String objectName) {
        // nothing to do
    }

}
