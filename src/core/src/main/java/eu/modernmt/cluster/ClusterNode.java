package eu.modernmt.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.*;
import eu.modernmt.cluster.db.DatabaseLoader;
import eu.modernmt.cluster.db.EmbeddedCassandra;
import eu.modernmt.cluster.error.FailedToJoinClusterException;
import eu.modernmt.cluster.kafka.EmbeddedKafka;
import eu.modernmt.cluster.kafka.KafkaDataManager;
import eu.modernmt.config.*;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.HostUnreachableException;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.decoder.DecoderWithFeatures;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.hw.NetworkUtils;
import eu.modernmt.persistence.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
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
    private DataManager dataManager;
    private Database database;
    private ITopic<Map<String, float[]>> decoderWeightsTopic;

    private ArrayList<EmbeddedService> services = new ArrayList<>(2);

    private final Thread shutdownThread = new Thread() {
        @Override
        public void run() {
            //TODO: must reintroduce valid model syncing
//            StorageService memory = StorageService.getInstance();
//            try {
//                memory.close();
//            } catch (IOException e) {
//                // Ignore exception
//            }

            forcefullyClose(executor);
            forcefullyClose(hazelcast);

            // Close engine resources
            forcefullyClose(engine);

            // Close services
            forcefullyClose(database);
            forcefullyClose(dataManager);

            for (EmbeddedService service : services)
                forcefullyClose(service);

            setStatus(Status.TERMINATED);
        }
    };

    private static void forcefullyClose(Closeable closeable) {
        try {
            if (closeable != null) closeable.close();
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static void forcefullyClose(ExecutorService executor) {
        try {
            if (executor != null) executor.shutdownNow();
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static void forcefullyClose(EmbeddedService service) {
        try {
            if (service != null) service.shutdown();
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static void forcefullyClose(HazelcastInstance hazelcast) {
        try {
            if (hazelcast != null) hazelcast.shutdown();
        } catch (Throwable e) {
            // Ignore
        }
    }

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

    public Database getDatabase() {
        if (database == null)
            throw new IllegalStateException("Database unavailable.");
        return database;
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

    private static void addToDataManager(DataListener listener, DataManager manager) {
        if (listener != null)
            manager.addDataListener(listener);
    }

    private static void addToDataManager(DataListenerProvider provider, DataManager manager) {
        if (provider != null) {
            for (DataListener listener : provider.getDataListeners())
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

        int executorPoolSize = nodeConfig.getEngineConfig().getDecoderConfig().getParallelismDegree();

        hazelcastConfig.getExecutorConfig(ClusterConstants.TRANSLATION_EXECUTOR_NAME)
                .setPoolSize(executorPoolSize)
                .setQueueCapacity(0);

        return hazelcastConfig;
    }

    public void start(NodeConfig nodeConfig) throws FailedToJoinClusterException, BootstrapException {
        start(nodeConfig, 0L, null);
    }

    public void start(NodeConfig nodeConfig, long joinTimeoutInterval, TimeUnit joinTimeoutUnit) throws FailedToJoinClusterException, BootstrapException {

        Timer globalTimer = new Timer();
        Timer timer = new Timer();

        // ===========  Join the cluster  =============

        setStatus(Status.JOINING);
        logger.info("Node is joining the cluster");
        Config hazelcastConfig = getHazelcastConfig(nodeConfig, joinTimeoutInterval, joinTimeoutUnit);
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


        // ===========  Adding shutdown hook for closing the cluster  =============

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ClusterNode.this.shutdown();
                ClusterNode.this.awaitTermination(1, TimeUnit.DAYS);
            } catch (Throwable e) {
                // Ignore
            }
        }));

        // ===========  Model syncing - not used  =============

        //TODO: must reintroduce valid model syncing
        logger.warn("Model syncing not supported in this version!");
//        if (args.member != null) {
        //        setStatus(Status.SYNCING);
//            InetAddress host = InetAddress.getByName(args.member);
//            File localPath = Engine.getRootPath(args.engine);
//
//            StorageService memory = StorageService.getInstance();
//            DirectorySynchronizer synchronizer = memory.getDirectorySynchronizer();
//            synchronizer.synchronize(host, args.dataPort, localPath);
//
//            status.onStatusChange(StatusManager.Status.SYNCHRONIZED);
//        setStatus(Status.SYNCED);
//        }


        // ===========  Model loading  =============

        setStatus(Status.LOADING);
        logger.info("Model loading started");

        timer.reset();
        this.engine = Engine.load(nodeConfig.getEngineConfig());
        setStatus(Status.LOADED);
        logger.info("Model loaded in " + (timer.time() / 1000.) + "s");


        // ===========  Data stream bootstrap  =============

        DataStreamConfig dataStreamConfig = nodeConfig.getDataStreamConfig();
        if (dataStreamConfig.isEnabled()) {

            boolean localDatastream = NetworkUtils.isLocalhost(dataStreamConfig.getHost());
            boolean embeddedDatastream = dataStreamConfig.isEmbedded();

            // if datastream is 'embedded' and datastream host is localhost,
            // start an instance of kafka process
            // else do nothing - will connect to a remote datastream
            // or to a local standalone datastream
            if (embeddedDatastream && localDatastream) {
                logger.info("Staring embedded Kafka process");
                timer.reset();

                EmbeddedKafka kafka = EmbeddedKafka.start(this.engine, dataStreamConfig.getPort());
                logger.info("Embedded Kafka started in " + (timer.time() / 1000.) + "s");

                this.services.add(kafka);
            }

            if (!embeddedDatastream && dataStreamConfig.getName() == null)
                throw new BootstrapException("Datastream name is compulsory if datastream is not embedded");


            this.dataManager = new KafkaDataManager(uuid, this.engine, dataStreamConfig);
            this.dataManager.setDataManagerListener(this::updateChannelsPositions);

            addToDataManager(this.engine, this.dataManager);
            updateChannelsPositions(this.dataManager.getChannelsPositions());

            try {
                timer.reset();

                logger.info("Connecting to dataManager...");
                Map<Short, Long> positions = dataManager.connect(dataStreamConfig.getHost(), dataStreamConfig.getPort(), 60, TimeUnit.SECONDS);
                logger.info("Connected to the dataManager in " + (timer.time() / 1000.) + "s");

                setStatus(Status.UPDATING);

                timer.reset();
                try {
                    logger.info("Starting sync from data stream");
                    this.dataManager.waitChannelPositions(positions);
                    logger.info("Data stream sync completed in " + (timer.time() / 1000.) + "s");
                } catch (InterruptedException e) {
                    throw new BootstrapException("Data stream sync interrupted", e);
                }

                setStatus(Status.UPDATED);
            } catch (HostUnreachableException e) {
                throw new BootstrapException("Unable to connect to DataManager", e);
            }
        }


        // ===========  Database bootstrap  =============

        DatabaseConfig databaseConfig = nodeConfig.getDatabaseConfig();
        if (databaseConfig.isEnabled()) {

            boolean localDatabase = NetworkUtils.isLocalhost(databaseConfig.getHost());
            boolean embeddedDatabase = databaseConfig.isEmbedded();

            // if db type is 'embedded' and db host is localhost, start a db process;
            // else do nothing - will connect to a remote db or a local standalone db
            if (embeddedDatabase && localDatabase) {
                logger.info("Starting embedded Cassandra process");
                timer.reset();

                EmbeddedCassandra cassandra = EmbeddedCassandra.start(engine, databaseConfig.getPort());
                logger.info("Embedded Cassandra started in " + (timer.time() / 1000.) + "s");

                this.services.add(cassandra);
            }

            if (!embeddedDatabase && databaseConfig.getName() == null)
                throw new BootstrapException("Database name is compulsory if database is not embedded");

            this.database = DatabaseLoader.load(engine, databaseConfig);
            //load may throw a bootstrap exception: just let it pass
        }


        // ========================

        executor = hazelcast.getExecutorService(ClusterConstants.TRANSLATION_EXECUTOR_NAME);

        decoderWeightsTopic = hazelcast.getTopic(ClusterConstants.DECODER_WEIGHTS_TOPIC_NAME);
        decoderWeightsTopic.addMessageListener(this::onDecoderWeightsChanged);

        setStatus(Status.READY);

        logger.info("Node started in " + (globalTimer.time() / 1000.) + "s");
    }

    public List<EmbeddedService> getServices() {
        return Collections.unmodifiableList(services);
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
        DecoderWithFeatures decoder = (DecoderWithFeatures) engine.getDecoder();
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

    private static class Timer {

        private long epoch = System.currentTimeMillis();

        public void reset() {
            epoch = System.currentTimeMillis();
        }

        public long time() {
            return System.currentTimeMillis() - epoch;
        }

    }
}
