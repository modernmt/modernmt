package eu.modernmt.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.*;
import eu.modernmt.aligner.Aligner;
import eu.modernmt.cluster.error.FailedToJoinClusterException;
import eu.modernmt.cluster.kafka.KafkaDataManager;
import eu.modernmt.config.DataStreamConfig;
import eu.modernmt.config.JoinConfig;
import eu.modernmt.config.NetworkConfig;
import eu.modernmt.config.NodeConfig;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.HostUnreachableException;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Callable;
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

    private Engine engine;

    private String uuid;
    private Status status;
    private ArrayList<StatusListener> statusListeners = new ArrayList<>();

    private HazelcastInstance hazelcast;
    private IExecutorService executor;
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
                executor.shutdownNow();
            hazelcast.shutdown();

            try {
                if (executor != null)
                    executor.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                // Ignore exception
            }

            // Close engine resources
            engine.close();

            setStatus(Status.TERMINATED);
        }
    };

    public ClusterNode() {
        this.status = Status.CREATED;
    }

    public Engine getEngine() {
        if (engine == null)
            throw new IllegalStateException("ClusterNode not ready. Call bootstrap() to initialize the member.");
        return engine;
    }

    public DataManager getDataManager() {
        if (dataManager == null)
            throw new UnsupportedOperationException("DataStream unavailable");

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

    private static void addToDataManager(Object object, DataManager manager) {
        if (object == null)
            return;

        if (object instanceof DataListener) {
            manager.addDataListener((DataListener) object);
        } else if (object instanceof DataListenerProvider) {
            for (DataListener listener : ((DataListenerProvider) object).getDataListeners())
                manager.addDataListener(listener);
        }
    }

    private Config getHazelcastConfig(NodeConfig nodeConfig, long interval, TimeUnit unit) {
        Config hazelcastConfig = new XmlConfigBuilder().build();

        NetworkConfig networkConfig = nodeConfig.getNetworkConfig();
        if (unit != null && interval > 0L) {
            long seconds = Math.max(unit.toSeconds(interval), 1L);
            hazelcastConfig.setProperty("hazelcast.max.join.seconds", Long.toString(seconds));
        }

        String listenInterface = networkConfig.getListeningInterface();
        if (listenInterface != null) {
            hazelcastConfig.getNetworkConfig().getInterfaces()
                    .setEnabled(true)
                    .addInterface(listenInterface);
        }

        hazelcastConfig.getNetworkConfig()
                .setPort(networkConfig.getPort());

        JoinConfig.Member[] members = networkConfig.getJoinConfig().getMembers();
        if (members != null && members.length > 0) {
            hazelcastConfig.setProperty("hazelcast.initial.min.cluster.size", "2");

            TcpIpConfig tcpIpConfig = hazelcastConfig.getNetworkConfig().getJoin().getTcpIpConfig();
            tcpIpConfig.setEnabled(true);

            for (JoinConfig.Member member : members)
                tcpIpConfig.addMember(member.getHost() + ":" + member.getPort());
        } else {
            hazelcastConfig.setProperty("hazelcast.initial.min.cluster.size", "1");
        }

        int executorPoolSize = nodeConfig.getEngineConfig().getDecoderConfig().getThreads();

        hazelcastConfig.getExecutorConfig(ClusterConstants.TRANSLATION_EXECUTOR_NAME)
                .setPoolSize(executorPoolSize)
                .setQueueCapacity(0);

        return hazelcastConfig;
    }

    public void start(NodeConfig nodeConfig) throws FailedToJoinClusterException, BootstrapException {
        start(nodeConfig, 0L, null);
    }

    public void start(NodeConfig nodeConfig, long joinTimeoutInterval, TimeUnit joinTimeoutUnit) throws FailedToJoinClusterException, BootstrapException {
        Config hazelcastConfig = getHazelcastConfig(nodeConfig, joinTimeoutInterval, joinTimeoutUnit);

        Timer globalTimer = new Timer();
        Timer timer = new Timer();

        // ========================

        setStatus(Status.JOINING);
        logger.info("Node is joining the cluster");

        timer.reset();
        try {
            hazelcast = Hazelcast.newHazelcastInstance(hazelcastConfig);
            uuid = hazelcast.getCluster().getLocalMember().getUuid();
        } catch (IllegalStateException e) {
            TcpIpConfig tcpIpConfig = hazelcastConfig.getNetworkConfig().getJoin().getTcpIpConfig();
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
        engine = Engine.load(nodeConfig.getEngineConfig());
        setStatus(Status.LOADED);
        logger.info("Model loaded in " + (timer.time() / 1000.) + "s");

        // ========================

        DataStreamConfig dataStreamConfig = nodeConfig.getDataStreamConfig();
        if (dataStreamConfig.isEnabled()) {
            dataManager = new KafkaDataManager(uuid, engine);
            dataManager.setDataManagerListener(this::updateChannelsPositions);

            Aligner aligner = engine.getAligner();
            Decoder decoder = engine.getDecoder();
            ContextAnalyzer contextAnalyzer = engine.getContextAnalyzer();

            addToDataManager(aligner, dataManager);
            addToDataManager(decoder, dataManager);
            addToDataManager(contextAnalyzer, dataManager);

            updateChannelsPositions(dataManager.getChannelsPositions());

            try {
                timer.reset();

                logger.info("Starting DataManager");
                Map<Short, Long> positions = dataManager.connect(dataStreamConfig.getHost(), dataStreamConfig.getPort(), 60, TimeUnit.SECONDS);
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
                throw new BootstrapException("Unable to connect to DataManager", e);
            }
        }

        // ========================

        executor = hazelcast.getExecutorService(ClusterConstants.TRANSLATION_EXECUTOR_NAME);

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
    }

    public Collection<NodeInfo> getClusterNodes() {
        Set<Member> members = hazelcast.getCluster().getMembers();
        ArrayList<NodeInfo> nodes = new ArrayList<>(members.size());

        for (Member member : hazelcast.getCluster().getMembers()) {
            nodes.add(NodeInfo.fromMember(member));
        }

        return nodes;
    }

    public <V> Future<V> submit(Callable<V> callable) {
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
