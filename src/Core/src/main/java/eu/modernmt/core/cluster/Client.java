package eu.modernmt.core.cluster;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import eu.modernmt.core.cluster.error.FailedToJoinClusterException;
import eu.modernmt.core.cluster.executor.DistributedCallable;
import eu.modernmt.core.cluster.executor.DistributedExecutor;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 20/04/16.
 */
public class Client {

    private HazelcastInstance hazelcast;
    private DistributedExecutor executor;
    private SessionManager sessionManager;

    public Client() {
    }

    public void joinCluster(String address) throws FailedToJoinClusterException {
        joinCluster(address, 0L, null);
    }

    public void joinCluster(String address, long duration, TimeUnit unit) throws FailedToJoinClusterException {
        int attemptPeriod = 3000;
        int attemptLimit = Integer.MAX_VALUE;

        if (unit != null) {
            int seconds = (int) unit.toSeconds(duration);

            if (seconds < 1) {
                attemptLimit = 1;
            } else {
                if (seconds % 3 == 0) {
                    attemptPeriod = 3000;
                    attemptLimit = seconds / 3;
                } else {
                    attemptPeriod = 2000;
                    attemptLimit = seconds / 2;
                }
            }
        }

        ClientConfig config = new XmlClientConfigBuilder().build();
        ClientNetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.addAddress(address);
        networkConfig.setConnectionAttemptLimit(attemptLimit);
        networkConfig.setConnectionAttemptPeriod(attemptPeriod);

        try {
            hazelcast = HazelcastClient.newHazelcastClient(config);
        } catch (IllegalStateException e) {
            throw new FailedToJoinClusterException(address);
        }

        this.executor = new DistributedExecutor(hazelcast, ClusterConstants.TRANSLATION_EXECUTOR_NAME);
        this.sessionManager = new SessionManager(hazelcast);
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void shutdown() {
        this.executor.shutdown();
        this.hazelcast.shutdown();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.executor.awaitTermination(timeout, unit);
    }

    public <V> Future<V> submit(DistributedCallable<V> callable) {
        return executor.submit(callable);
    }
}
