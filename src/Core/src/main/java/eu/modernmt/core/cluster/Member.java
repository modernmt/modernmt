package eu.modernmt.core.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import eu.modernmt.core.Engine;
import eu.modernmt.core.LazyLoadException;
import eu.modernmt.core.cluster.error.BootstrapException;
import eu.modernmt.core.cluster.error.FailedToJoinClusterException;
import eu.modernmt.core.cluster.executor.ExecutorDaemon;
import eu.modernmt.core.config.EngineConfig;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 18/04/16.
 */
public class Member {

    private static final int SHUTDOWN_NOT_INVOKED = 0;
    private static final int SHUTDOWN_INVOKED = 1;
    private static final int SHUTDOWN_COMPLETED = 2;

    private int shutdownState = SHUTDOWN_NOT_INVOKED;

    private final Thread shutdownThread = new Thread() {
        @Override
        public void run() {
            if (executorDaemon != null)
                executorDaemon.shutdown();
            hazelcast.shutdown();

            try {
                if (executorDaemon != null)
                    executorDaemon.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                // Ignore exception
            }

            shutdownState = SHUTDOWN_COMPLETED;
        }
    };

    private final int port;
    private final int capacity;
    private Engine engine;

    private HazelcastInstance hazelcast;
    private ExecutorDaemon executorDaemon;
    private SessionManager sessionManager;

    public Member(int port) {
        this(port, ClusterConstants.DEFAULT_TRANSLATION_EXECUTOR_SIZE);
    }

    public Member(int port, int capacity) {
        this.port = port;
        this.capacity = capacity;
    }

    public Engine getEngine() {
        if (engine == null)
            throw new IllegalStateException("Member not ready. Call bootstrap() to initialize the member.");
        return engine;
    }

    public void startCluster() {
        Config config = new XmlConfigBuilder().build();
        config.getNetworkConfig().setPort(port);
        config.setProperty("hazelcast.initial.min.cluster.size", "1");

        hazelcast = Hazelcast.newHazelcastInstance(config);
    }

    public void joinCluster(String address) throws FailedToJoinClusterException {
        joinCluster(address, null, 0L);
    }

    public void joinCluster(String address, TimeUnit unit, long interval) throws FailedToJoinClusterException {
        Config config = new XmlConfigBuilder().build();
        config.getNetworkConfig().setPort(port);
        config.setProperty("hazelcast.initial.min.cluster.size", "2");

        if (unit != null) {
            long seconds = Math.max(unit.toSeconds(interval), 1L);
            config.setProperty("hazelcast.max.join.seconds", Long.toString(seconds));
        }

        TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
        tcpIpConfig.setRequiredMember(address);

        try {
            hazelcast = Hazelcast.newHazelcastInstance(config);
        } catch (IllegalStateException e) {
            throw new FailedToJoinClusterException(address);
        }
    }

    public void bootstrap(EngineConfig config) throws BootstrapException {
        File workingDirectory = eu.modernmt.config.Config.fs.getRuntime(config.getName(), "slave");
        engine = new Engine(config, capacity);
        engine.setWorkingDirectory(workingDirectory);

        try {
            engine.getAligner();
            engine.getDecoder();
            engine.getContextAnalyzer();
            engine.getPreprocessor();
            engine.getPostprocessor();
        } catch (LazyLoadException e) {
            throw new BootstrapException(e.getCause());
        }

        executorDaemon = new ExecutorDaemon(hazelcast, this, ClusterConstants.TRANSLATION_EXECUTOR_NAME, capacity);
        sessionManager = new SessionManager(hazelcast);
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public synchronized void shutdown() {
        if (shutdownState == SHUTDOWN_NOT_INVOKED) {
            shutdownState = SHUTDOWN_INVOKED;
            shutdownThread.start();
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (shutdownState == SHUTDOWN_INVOKED)
            unit.timedJoin(shutdownThread, timeout);
        return !shutdownThread.isAlive();
    }

}
