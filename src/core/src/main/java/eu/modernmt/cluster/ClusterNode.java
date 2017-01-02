package eu.modernmt.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.*;
import eu.modernmt.aligner.Aligner;
import eu.modernmt.cluster.kafka.KafkaDataManager;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.HostUnreachableException;
import eu.modernmt.cluster.error.BootstrapException;
import eu.modernmt.cluster.error.FailedToJoinClusterException;
import eu.modernmt.cluster.executor.DistributedCallable;
import eu.modernmt.cluster.executor.DistributedExecutor;
import eu.modernmt.cluster.executor.ExecutorDaemon;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.engine.Engine;
import eu.modernmt.engine.LazyLoadException;
import eu.modernmt.engine.config.EngineConfig;
import eu.modernmt.engine.config.INIEngineConfigWriter;
import eu.modernmt.data.DataListener;
import eu.modernmt.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 18/04/16.
 */
public class ClusterNode {

    public enum Status {
        CREATED,        // Node has just been created
        JOINING,        // Node is joining the cluster
        JOINED,         // Node has joined the cluster
        SYNCHRONIZING,  // Node is downloading latest models snapshot
        SYNCHRONIZED,   // Node downloaded latest models snapshot
        LOADING,        // Node is loading the models
        LOADED,         // Node loaded the models
        UPDATING,       // Node is updating its models with the latest contributions
        UPDATED,        // Node updated its models with the latest contributions
        READY,          // Node is ready and can receive translation requests
        SHUTDOWN,       // Node is shutting down
        TERMINATED      // Node is no longer active
    }

    public interface StatusListener {

        void onStatusChanged(ClusterNode node, Status currentStatus, Status previousStatus);

    }

    private final Logger logger = LogManager.getLogger(ClusterNode.class);

    private final int controlPort;
    private final int dataPort;
    private final Engine engine;
    private String uuid;
    private Status status;
    private ArrayList<StatusListener> statusListeners = new ArrayList<>();

    private HazelcastInstance hazelcast;
    private ExecutorDaemon executorDaemon;
    private DistributedExecutor executor;
    private SessionManager sessionManager;
    private DataManager dataManager;
    private ITopic<Map<String, float[]>> decoderWeightsTopic;

    private final Thread shutdownThread = new Thread() {
        @Override
        public void run() {
            //TODO: must reintroduce valid model syncing
//            StorageService storage = StorageService.getInstance();
//            try {
//                storage.close();
//            } catch (IOException e) {
//                // Ignore exception
//            }

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

            // Close engine resources
            engine.close();

            setStatus(Status.TERMINATED);
        }
    };

    public ClusterNode(Engine engine, int controlPort, int dataPort) {
        this.controlPort = controlPort;
        this.dataPort = dataPort;
        this.engine = engine;

        this.status = Status.CREATED;
    }

    public Engine getEngine() {
        if (engine == null)
            throw new IllegalStateException("ClusterNode not ready. Call bootstrap() to initialize the member.");
        return engine;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void addStatusListener(StatusListener listener) {
        this.statusListeners.add(listener);
    }

    public void removeStatusListener(StatusListener listener) {
        this.statusListeners.remove(listener);
    }

    private void setStatus(Status status) {
        setStatus(status, null);
    }

    private synchronized boolean setStatus(Status status, Status expected) {
        if (expected == null || this.status == expected) {
            Status previousStatus = this.status;
            this.status = status;

            if (this.hazelcast != null) {
                Member localMember = this.hazelcast.getCluster().getLocalMember();
                NodeInfo.updateStatusInMember(localMember, status);
            }

            if (logger.isDebugEnabled())
                logger.debug("Cluster node status changed: " + previousStatus + " -> " + status);

            for (StatusListener listener : statusListeners) {
                try {
                    listener.onStatusChanged(this, this.status, previousStatus);
                } catch (RuntimeException e) {
                    logger.error("Unexpected exception while updating Node status. Resuming normal operations.", e);
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public synchronized Status getStatus() {
        return status;
    }

    // Cluster startup

    private Config getHazelcastConfig(String member, long interval, TimeUnit unit) {
        Config config = new XmlConfigBuilder().build();
        config.getNetworkConfig().setPort(controlPort);

        config.setProperty("hazelcast.initial.min.cluster.size", member == null ? "1" : "2");

        if (unit != null) {
            long seconds = Math.max(unit.toSeconds(interval), 1L);
            config.setProperty("hazelcast.max.join.seconds", Long.toString(seconds));
        }

        if (member != null) {
            TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
            tcpIpConfig.setRequiredMember(member);
        }

        return config;
    }

    public void startCluster() throws FailedToJoinClusterException, BootstrapException {
        Config config = getHazelcastConfig(null, 0L, null);
        start(config);
    }

    public void joinCluster(String address) throws FailedToJoinClusterException, BootstrapException {
        joinCluster(address, 0L, null);
    }

    public void joinCluster(String address, long interval, TimeUnit unit) throws FailedToJoinClusterException, BootstrapException {
        Config config = getHazelcastConfig(address, interval, unit);
        start(config);
    }

    public Collection<NodeInfo> getClusterNodes() {
        Set<Member> members = hazelcast.getCluster().getMembers();
        ArrayList<NodeInfo> nodes = new ArrayList<>(members.size());

        for (Member member : hazelcast.getCluster().getMembers()) {
            nodes.add(NodeInfo.fromMember(member));
        }

        return nodes;
    }

    private void start(Config config) throws FailedToJoinClusterException, BootstrapException {
        Timer globalTimer = new Timer();
        Timer timer = new Timer();

        // ========================

        setStatus(Status.JOINING);
        logger.info("Node is joining the cluster");

        timer.reset();
        try {
            hazelcast = Hazelcast.newHazelcastInstance(config);
            uuid = hazelcast.getCluster().getLocalMember().getUuid();
        } catch (IllegalStateException e) {
            TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
            throw new FailedToJoinClusterException(tcpIpConfig.getRequiredMember());
        }

        setStatus(Status.JOINED);
        logger.info("Node joined the cluster in " + (timer.time() / 1000.) + "s");

        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        // ========================

        //TODO: must reintroduce valid model syncing
        logger.warn("Model syncing not supported in this version!");
//        if (args.member != null) {
        //        setStatus(Status.SYNCING);
//            InetAddress host = InetAddress.getByName(args.member);
//            File localPath = Engine.getRootPath(args.engine);
//
//            StorageService storage = StorageService.getInstance();
//            DirectorySynchronizer synchronizer = storage.getDirectorySynchronizer();
//            synchronizer.synchronize(host, args.dataPort, localPath);
//
//            status.onStatusChange(StatusManager.Status.SYNCHRONIZED);
//        setStatus(Status.SYNCED);
//        }

        // ========================

        setStatus(Status.LOADING);
        logger.info("Model loading started");

        timer.reset();
        try {
            engine.loadModels();
        } catch (LazyLoadException e) {
            throw new BootstrapException(e.getCause());
        }

        setStatus(Status.LOADED);
        logger.info("Model loaded in " + (timer.time() / 1000.) + "s");

        // ========================

        dataManager = new KafkaDataManager(uuid, engine);
        dataManager.setDataManagerListener(this::updateChannelsPositions);

        Aligner aligner = engine.getAligner();
        Decoder decoder = engine.getDecoder();
        ContextAnalyzer contextAnalyzer = engine.getContextAnalyzer();

        if (aligner instanceof DataListener)
            dataManager.addDataListener((DataListener) aligner);
        if (decoder instanceof DataListener)
            dataManager.addDataListener((DataListener) decoder);
        if (contextAnalyzer instanceof DataListener)
            dataManager.addDataListener((DataListener) contextAnalyzer);

        updateChannelsPositions(dataManager.getChannelsPositions());

        try {
            timer.reset();

            logger.info("Starting DataManager");
            // TODO: should read host and ports from config
            Map<Short, Long> positions = dataManager.connect(10, TimeUnit.SECONDS);
            logger.info("DataManager ready in " + (timer.time() / 1000.) + "s");

            setStatus(Status.UPDATING);

            timer.reset();
            try {
                logger.info("Starting sync from data stream");
                dataManager.waitChannelPositions(positions);
                logger.info("Data stream sync completed in " + (timer.time() / 1000.) + "s");
            } catch (InterruptedException e) {
                throw new BootstrapException("Data stream sync interrupted", e);
            }

            setStatus(Status.UPDATED);
        } catch (HostUnreachableException e) {
            logger.error("Unable to connect to DataManager", e);
        }

        // ========================

        int executorCapacity = engine.getConfig().getDecoderConfig().getThreads();

        executor = new DistributedExecutor(hazelcast, ClusterConstants.TRANSLATION_EXECUTOR_NAME);
        executorDaemon = new ExecutorDaemon(hazelcast, this, ClusterConstants.TRANSLATION_EXECUTOR_NAME, executorCapacity);

        sessionManager = new SessionManager(hazelcast, event -> engine.getDecoder().closeSession(event.getOldValue()));

        decoderWeightsTopic = hazelcast.getTopic(ClusterConstants.DECODER_WEIGHTS_TOPIC_NAME);
        decoderWeightsTopic.addMessageListener(this::onDecoderWeightsChanged);

        setStatus(Status.READY);

        logger.info("Node started in " + (globalTimer.time() / 1000.) + "s");
    }

    private void updateChannelsPositions(Map<Short, Long> positions) {
        Member localMember = hazelcast.getCluster().getLocalMember();
        NodeInfo.updateChannelsPositionsInMember(localMember, positions);
    }

    public void notifyDecoderWeightsChanged(Map<String, float[]> weights) {
        this.decoderWeightsTopic.publish(weights);
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

    public <V> Future<V> submit(DistributedCallable<V> callable) {
        return executor.submit(callable);
    }

    public synchronized void shutdown() {
        if (setStatus(Status.SHUTDOWN, Status.READY))
            shutdownThread.start();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (getStatus() == Status.SHUTDOWN)
            unit.timedJoin(shutdownThread, timeout);

        return !shutdownThread.isAlive();
    }

    private class ShutdownHook extends Thread {

        @Override
        public void run() {
            try {
                ClusterNode.this.shutdown();
            } catch (Throwable e) {
                // Ignore
            }
            try {
                ClusterNode.this.awaitTermination(1, TimeUnit.DAYS);
            } catch (Throwable e) {
                // Ignore
            }
        }

    }

}
