package eu.modernmt.core.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import eu.modernmt.core.Engine;
import eu.modernmt.core.LazyLoadException;
import eu.modernmt.core.cluster.error.BootstrapException;
import eu.modernmt.core.cluster.error.FailedToJoinClusterException;
import eu.modernmt.core.cluster.executor.DistributedCallable;
import eu.modernmt.core.cluster.executor.DistributedExecutor;
import eu.modernmt.core.cluster.executor.ExecutorDaemon;
import eu.modernmt.core.cluster.storage.StorageService;
import eu.modernmt.core.config.EngineConfig;
import eu.modernmt.core.config.INIEngineConfigWriter;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 18/04/16.
 */
public class ClusterNode {

    private static final int SHUTDOWN_NOT_INVOKED = 0;
    private static final int SHUTDOWN_INVOKED = 1;
    private static final int SHUTDOWN_COMPLETED = 2;

    private int shutdownState = SHUTDOWN_NOT_INVOKED;

    private final Thread shutdownThread = new Thread() {
        @Override
        public void run() {
            StorageService storage = StorageService.getInstance();
            try {
                storage.close();
            } catch (IOException e) {
                // Ignore exception
            }

            if (executor != null)
                executor.shutdown();
            if (executorDaemon != null)
                executorDaemon.shutdown();
            hazelcast.shutdown();

            try {
                if (executor != null)
                    executor.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                // Ignore exception
            }

            try {
                if (executorDaemon != null)
                    executorDaemon.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                // Ignore exception
            }

            shutdownState = SHUTDOWN_COMPLETED;
        }
    };

    private final Logger logger = LogManager.getLogger(ClusterNode.class);

    private final int controlPort;
    private final int dataPort;
    private final int capacity;
    private Engine engine;

    private HazelcastInstance hazelcast;
    private ExecutorDaemon executorDaemon;
    private DistributedExecutor executor;
    private SessionManager sessionManager;
    private ITopic<Map<String, float[]>> decoderWeightsTopic;

    public ClusterNode(int controlPort, int dataPort) {
        this(controlPort, dataPort, ClusterConstants.DEFAULT_TRANSLATION_EXECUTOR_SIZE);
    }

    public ClusterNode(int controlPort, int dataPort, int capacity) {
        this.controlPort = controlPort;
        this.dataPort = dataPort;
        this.capacity = capacity;
    }

    public Engine getEngine() {
        if (engine == null)
            throw new IllegalStateException("ClusterNode not ready. Call bootstrap() to initialize the member.");
        return engine;
    }

    public void startCluster() {
        Config config = new XmlConfigBuilder().build();
        config.getNetworkConfig().setPort(controlPort);
        config.setProperty("hazelcast.initial.min.cluster.size", "1");

        logger.info("Starting cluster");
        hazelcast = Hazelcast.newHazelcastInstance(config);
        logger.info("Cluster successfully started");
    }

    public void joinCluster(String address) throws FailedToJoinClusterException {
        joinCluster(address, 0L, null);
    }

    public void joinCluster(String address, long interval, TimeUnit unit) throws FailedToJoinClusterException {
        Config config = new XmlConfigBuilder().build();
        config.getNetworkConfig().setPort(controlPort);
        config.setProperty("hazelcast.initial.min.cluster.size", "2");

        if (unit != null) {
            long seconds = Math.max(unit.toSeconds(interval), 1L);
            config.setProperty("hazelcast.max.join.seconds", Long.toString(seconds));
        }

        TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
        tcpIpConfig.setRequiredMember(address);

        try {
            logger.info("Joining cluster");

            hazelcast = Hazelcast.newHazelcastInstance(config);

            int members = hazelcast.getCluster().getMembers().size();
            logger.info("Cluster successfully joined with " + members + "members");
        } catch (IllegalStateException e) {
            throw new FailedToJoinClusterException(address);
        }
    }

    public void bootstrap(EngineConfig config) throws BootstrapException {
        logger.info("Starting member bootstrap");
        engine = new Engine(config, capacity);

        StorageService storage = StorageService.getInstance();
        try {
            storage.start(this.dataPort, engine);
        } catch (IOException e) {
            throw new BootstrapException(e);
        }

        try {
            engine.getAligner();
            engine.getDecoder();
            engine.getContextAnalyzer();
            engine.getPreprocessor();
            engine.getPostprocessor();
        } catch (LazyLoadException e) {
            throw new BootstrapException(e.getCause());
        }

        executor = new DistributedExecutor(hazelcast, ClusterConstants.TRANSLATION_EXECUTOR_NAME);
        executorDaemon = new ExecutorDaemon(hazelcast, this, ClusterConstants.TRANSLATION_EXECUTOR_NAME, capacity);
        sessionManager = new SessionManager(hazelcast, event -> engine.getDecoder().closeSession(event.getOldValue()));
        decoderWeightsTopic = hazelcast.getTopic(ClusterConstants.DECODER_WEIGHTS_TOPIC_NAME);
        decoderWeightsTopic.addMessageListener(this::onDecoderWeightsChanged);

        logger.info("Node bootstrap completed, all models loaded");
    }

    private void onDecoderWeightsChanged(Message<Map<String, float[]>> message) {
        logger.info("Received decoder weights changed notification");

        Map<String, float[]> weights = message.getMessageObject();

        // Updating decoder weights
        Decoder decoder = engine.getDecoder();
        Map<DecoderFeature, float[]> map = new HashMap<>();

        for (DecoderFeature feature : decoder.getFeatures()) {
            if (feature.isTunable())
                map.put(feature, weights.get(feature.getName()));
        }

        decoder.setDefaultFeatureWeights(map);

        // Updating engine.ini
        EngineConfig config = engine.getConfig();
        config.getDecoderConfig().setWeights(weights);

        File file = Engine.getConfigFile(engine.getName());

        try {
            new INIEngineConfigWriter(config).write(file);
            logger.info("Engine's config file successfully written");
        } catch (IOException e) {
            throw new RuntimeException("Unable to write config file: " + file, e);
        }
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void notifyDecoderWeightsChanged(Map<String, float[]> weights) {
        this.decoderWeightsTopic.publish(weights);
    }

    public <V> Future<V> submit(DistributedCallable<V> callable) {
        return executor.submit(callable);
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
