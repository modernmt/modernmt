package eu.modernmt.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import eu.modernmt.api.ApiServer;
import eu.modernmt.cluster.cassandra.EmbeddedCassandra;
import eu.modernmt.cluster.error.FailedToJoinClusterException;
import eu.modernmt.cluster.kafka.EmbeddedKafka;
import eu.modernmt.cluster.kafka.KafkaBinaryLog;
import eu.modernmt.cluster.services.TranslationService;
import eu.modernmt.cluster.services.TranslationServiceProxy;
import eu.modernmt.config.*;
import eu.modernmt.data.LogDataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.data.BinaryLog;
import eu.modernmt.data.HostUnreachableException;
import eu.modernmt.decoder.DecoderListener;
import eu.modernmt.decoder.DecoderUnavailableException;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.hw.NetworkUtils;
import eu.modernmt.lang.LanguageBridge;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Translation;
import eu.modernmt.persistence.Database;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.persistence.cassandra.CassandraDatabase;
import eu.modernmt.persistence.mysql.MySQLDatabase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        RUNNING,        // Node is running and it can receive translation requests
        DEGRADED,       // Node has some decoder processes and it is recovering (CAN handle translation requests)
        UNAVAILABLE,    // Node has no decoder processes and it is recovering (CANNOT handle translation requests)
        SHUTDOWN,       // Node is shutting down
        TERMINATED,     // Node is no longer active

        UNKNOWN         // Node has no valid status (or null)
    }

    public interface StatusListener {
        void onStatusChanged(ClusterNode node, Status currentStatus, Status previousStatus);
    }

    private final Logger logger = LogManager.getLogger(ClusterNode.class);

    private Engine engine;

    private Status status;
    private ArrayList<StatusListener> statusListeners = new ArrayList<>();

    HazelcastInstance hazelcast;
    BinaryLog binaryLog;
    Database database;
    ApiServer api;
    TranslationServiceProxy translationService;
    ArrayList<EmbeddedService> services = new ArrayList<>(2);

    private final ShutdownThread shutdownThread = new ShutdownThread(this);
    private boolean loadBalancing = true;
    private boolean isShuttingDown = false;

    public ClusterNode() {
        this.status = Status.CREATED;
    }

    public Engine getEngine() {
        if (engine == null)
            throw new IllegalStateException("ClusterNode not ready. Call bootstrap() to initialize the member.");
        return engine;
    }

    public BinaryLog getBinaryLog() {
        if (binaryLog == null)
            throw new UnsupportedOperationException("BinaryLog unavailable");
        return binaryLog;
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

    void setStatus(Status status) {
        setStatus(status, (Status[]) null);
    }

    private boolean setStatus(Status status, Status... expected) {
        if (expected == null || contains(expected, this.status)) {
            synchronized (this) {
                if (expected == null || contains(expected, this.status)) {
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
        } else {
            return false;
        }
    }

    public synchronized Status getStatus() {
        return status;
    }

    // Cluster startup

    private static void addToBinaryLog(LogDataListener listener, BinaryLog binlog) {
        if (listener != null)
            binlog.addLogDataListener(listener);
    }

    private static void addToBinaryLog(DataListenerProvider provider, BinaryLog binlog) {
        if (provider != null) {
            for (LogDataListener listener : provider.getDataListeners())
                binlog.addLogDataListener(listener);
        }
    }

    private Config getHazelcastConfig(NodeConfig nodeConfig, long interval, TimeUnit unit) {
        Config hazelcastConfig = new XmlConfigBuilder().build();

        NetworkConfig networkConfig = nodeConfig.getNetworkConfig();
        if (unit != null && interval > 0L) {
            long seconds = Math.max(unit.toSeconds(interval), 1L);
            hazelcastConfig.setProperty("hazelcast.max.join.seconds", Long.toString(seconds));
        }

        String host = networkConfig.getHost();
        if (host != null)
            hazelcastConfig.getNetworkConfig().setPublicAddress(host);

        hazelcastConfig.getNetworkConfig().setPort(networkConfig.getPort());

        String listenInterface = networkConfig.getListeningInterface();
        if (listenInterface != null) {
            hazelcastConfig.getNetworkConfig().getInterfaces()
                    .setEnabled(true)
                    .addInterface(listenInterface);
            hazelcastConfig.setProperty("hazelcast.local.localAddress", listenInterface);
            hazelcastConfig.setProperty("hazelcast.local.publicAddress", listenInterface);
        }

        JoinConfig joinConfig = networkConfig.getJoinConfig();
        JoinConfig.Member[] members = joinConfig.getMembers();
        if (members != null && members.length > 0) {
            TcpIpConfig tcpIpConfig = hazelcastConfig.getNetworkConfig().getJoin().getTcpIpConfig();
            tcpIpConfig.setConnectionTimeoutSeconds(joinConfig.getTimeout());
            tcpIpConfig.setEnabled(true);

            for (JoinConfig.Member member : members)
                tcpIpConfig.addMember(member.getHost() + ":" + member.getPort());
        }

        return hazelcastConfig;
    }

    public void start(NodeConfig nodeConfig) throws FailedToJoinClusterException, BootstrapException {
        start(nodeConfig, 0L, null);
    }

    public void start(NodeConfig nodeConfig, long joinTimeoutInterval, TimeUnit joinTimeoutUnit) throws FailedToJoinClusterException, BootstrapException {
        logger.info("Starting node with config:\n" + nodeConfig.toString());

        this.loadBalancing = nodeConfig.isLoadBalancingActive();

        Timer globalTimer = new Timer();
        Timer timer = new Timer();

        String uuid;

        // ===========  Adding shutdown hook for closing the cluster  =============

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ClusterNode.this.shutdown();
                ClusterNode.this.awaitTermination(1, TimeUnit.DAYS);
            } catch (Throwable e) {
                // Ignore
            }
        }));

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
        logger.info("Node joined cluster of " + countClusterMembers(false) + " members in " + (timer.time() / 1000.) + "s");

        // ===========  REST Api start ===========

        ApiConfig apiConfig = nodeConfig.getNetworkConfig().getApiConfig();

        if (apiConfig.isEnabled()) {
            ApiServer.ServerOptions options = new ApiServer.ServerOptions(apiConfig.getPort());
            options.contextPath = apiConfig.getApiRoot();

            this.api = new ApiServer(options);
            try {
                this.api.start();
            } catch (Exception e) {
                throw new BootstrapException("Unable to start REST Api service", e);
            }
        }

        // ===========  Model loading  =============

        setStatus(Status.LOADING);
        logger.info("Model loading started");

        timer.reset();
        this.engine = Engine.load(nodeConfig.getEngineConfig());
        try {
            this.engine.getDecoder().setListener(new DecoderListener() {
                @Override
                public void onTranslationDirectionsChanged(Set<LanguageDirection> directions) {
                    updateDecoderTranslationDirections(directions);
                }

                @Override
                public void onDecoderAvailabilityChanged(int currentAvailability, int maxAvailability) {
                    updateDecoderAvailability(currentAvailability, maxAvailability);
                }
            });
        } catch (UnsupportedOperationException e) {
            // Ignore, decoder not available
        }
        setStatus(Status.LOADED);
        logger.info("Model loaded in " + (timer.time() / 1000.) + "s");


        // ===========  Data stream bootstrap  =============

        BinaryLogConfig binaryLogConfig = nodeConfig.getBinaryLogConfig();
        if (binaryLogConfig.isEnabled()) {
            String[] hosts = binaryLogConfig.getHosts();
            boolean localBinaryLog = hosts.length == 1 && NetworkUtils.isLocalhost(hosts[0]);
            boolean embeddedBinaryLog = binaryLogConfig.isEmbedded();

            // if binlog is 'embedded' and binlog host is localhost,
            // start an instance of kafka process
            // else do nothing - will connect to a remote binlog
            // or to a local standalone binlog
            if (embeddedBinaryLog && localBinaryLog) {
                logger.info("Starting embedded Kafka process");
                timer.reset();

                String host = hazelcast.getCluster().getLocalMember().getAddress().getHost();
                binaryLogConfig.setHost(host);

                EmbeddedKafka kafka = EmbeddedKafka.start(this.engine, host, binaryLogConfig.getPort());
                logger.info("Embedded Kafka started in " + (timer.time() / 1000.) + "s");

                this.services.add(kafka);
            }

            if (!embeddedBinaryLog && binaryLogConfig.getName() == null)
                throw new BootstrapException("BinaryLog name is mandatory if binlog is not embedded");

            this.binaryLog = new KafkaBinaryLog(this.engine, uuid, binaryLogConfig);
            this.binaryLog.setBinaryLogListener(this::updateChannelsPositions);

            addToBinaryLog(this.engine, this.binaryLog);
            updateChannelsPositions(this.binaryLog.getChannelsPositions());

            try {
                timer.reset();

                logger.info("Connecting to binary log server...");
                Map<Short, Long> positions = binaryLog.connect();
                logger.info("Connected to the binary log server in " + (timer.time() / 1000.) + "s");

                setStatus(Status.UPDATING);

                if (countClusterMembers(true) > 0) {
                    timer.reset();
                    try {
                        logger.info("Starting sync from binary log");
                        this.binaryLog.waitChannelPositions(positions);
                        logger.info("Binary log sync completed in " + (timer.time() / 1000.) + "s");
                    } catch (InterruptedException e) {
                        throw new BootstrapException("Binary log sync interrupted", e);
                    }
                } else {
                    logger.info("Binary log sync running in background, force single running node cluster start");
                }

                setStatus(Status.UPDATED);
            } catch (HostUnreachableException e) {
                throw new BootstrapException("Unable to connect to BinaryLog", e);
            }
        }


        // ===========  Database bootstrap  =============

        DatabaseConfig databaseConfig = nodeConfig.getDatabaseConfig();
        if (databaseConfig.isEnabled()) {
            boolean local = NetworkUtils.isLocalhost(databaseConfig.getHost());
            boolean embedded = databaseConfig.isEmbedded();

            // if cassandra is 'embedded' and cassandra host is localhost, start a cassandra process;
            // else do nothing - will connect to a remote cassandra or a local standalone cassandra
            if (embedded && local && databaseConfig.getType() == DatabaseConfig.Type.CASSANDRA) {
                logger.info("Starting embedded Cassandra process");
                timer.reset();

                EmbeddedCassandra cassandra = EmbeddedCassandra.start(engine, databaseConfig.getPort());
                logger.info("Embedded Cassandra started in " + (timer.time() / 1000.) + "s");

                this.services.add(cassandra);
            }

            if (!embedded && databaseConfig.getName() == null)
                throw new BootstrapException("Database name is mandatory if database is not embedded");

            switch (databaseConfig.getType()) {
                case CASSANDRA:
                    this.database = new CassandraDatabase(databaseConfig);
                    break;
                case MYSQL:
                    this.database = new MySQLDatabase(databaseConfig);
                    break;
                default:
                    throw new Error("Invalid value for enum DatabaseConfig.Type: " + databaseConfig.getType());
            }

            try {
                if (!this.database.exists())
                    this.database.create();
            } catch (PersistenceException e) {
                throw new BootstrapException("Failed to create database: " + this.database.getClass().getSimpleName(), e);
            }
        }


        // ===========  Hazelcast services init =============

        translationService = hazelcast.getDistributedObject(TranslationService.SERVICE_NAME, "TranslationService");

        setStatus(Status.RUNNING);
        logger.info("Node started in " + (globalTimer.time() / 1000.) + "s");
    }

    private int countClusterMembers(boolean onlyRunning) {
        int size = 0;

        for (Member member : hazelcast.getCluster().getMembers()) {
            if (!onlyRunning || NodeInfo.statusIs(member, Status.RUNNING))
                size++;
        }

        return size;
    }

    public List<EmbeddedService> getServices() {
        return Collections.unmodifiableList(services);
    }

    private void updateChannelsPositions(Map<Short, Long> positions) {
        Member localMember = hazelcast.getCluster().getLocalMember();
        NodeInfo.updateChannelsPositionsInMember(localMember, positions);
    }

    private void updateDecoderTranslationDirections(Set<LanguageDirection> directions) {
        Member localMember = hazelcast.getCluster().getLocalMember();
        NodeInfo.updateTranslationDirections(localMember, directions);
    }

    private void updateDecoderAvailability(int currentAvailability, int maxAvailability) {
        if (currentAvailability == 0)
            setStatus(Status.UNAVAILABLE, Status.RUNNING, Status.DEGRADED);
        else if (currentAvailability < maxAvailability)
            setStatus(Status.DEGRADED, Status.RUNNING, Status.UNAVAILABLE);
        else
            setStatus(Status.RUNNING, Status.UNAVAILABLE, Status.DEGRADED);
    }

    public Collection<NodeInfo> getClusterNodes() {
        Set<Member> members = hazelcast.getCluster().getMembers();
        ArrayList<NodeInfo> nodes = new ArrayList<>(members.size());

        for (Member member : hazelcast.getCluster().getMembers()) {
            nodes.add(NodeInfo.fromMember(member));
        }

        return nodes;
    }

    public NodeInfo getLocalNode() {
        Member member = hazelcast.getCluster().getLocalMember();
        return NodeInfo.fromMember(member);
    }

    public Future<Translation> submit(TranslationTask task) throws DecoderUnavailableException {
        LanguageDirection language = task.getLanguageDirection();
        LanguageBridge bridge = engine.getLanguageIndex().getLanguageBridge(language);

        Member member;
        if (this.loadBalancing) {
            member = getRandomMember(language, bridge);
        } else {
            member = hazelcast.getCluster().getLocalMember();
            if (!NodeInfo.statusIs(member, Status.RUNNING, Status.DEGRADED))
                throw new DecoderUnavailableException("Local node is not active");
            if (!hasTranslationDirection(member, language, bridge))
                throw new UnsupportedLanguageException(language);
        }

        return translationService.submit(task, member.getAddress());
    }

    private Member getRandomMember(LanguageDirection language, LanguageBridge bridge) throws DecoderUnavailableException {
        Set<Member> members = hazelcast.getCluster().getMembers();

        ArrayList<Member> candidates = new ArrayList<>(members.size());
        int activeNodes = 0;

        for (Member member : members) {
            if (!NodeInfo.statusIs(member, Status.RUNNING, Status.DEGRADED))
                continue;

            activeNodes++;

            if (hasTranslationDirection(member, language, bridge))
                candidates.add(member);
        }

        if (candidates.isEmpty()) {
            if (activeNodes > 0)
                throw new UnsupportedLanguageException(language);
            else
                throw new DecoderUnavailableException("Could not find active node in the cluster");
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        } else {
            int i = new Random().nextInt(candidates.size());
            return candidates.get(i);
        }
    }

    private static boolean hasTranslationDirection(Member member, LanguageDirection language, LanguageBridge bridge) {
        if (bridge == null)
            return NodeInfo.hasTranslationDirection(member, language);
        else
            return NodeInfo.hasTranslationDirection(member, bridge.source) && NodeInfo.hasTranslationDirection(member, bridge.target);
    }

    public synchronized void shutdown() {
        if (isShuttingDown)
            return;

        isShuttingDown = true;
        setStatus(Status.SHUTDOWN);
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

    private static boolean contains(Status[] haystack, Status needle) {
        for (Status e : haystack) {
            if (e == needle)
                return true;
        }

        return false;
    }
}
