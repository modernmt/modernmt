package eu.modernmt.facade;

import eu.modernmt.Pom;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.NodeInfo;
import eu.modernmt.cluster.ServerInfo;
import eu.modernmt.cluster.error.FailedToJoinClusterException;
import eu.modernmt.config.NodeConfig;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.facade.exceptions.TestFailedException;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.TranslationMemory;
import eu.modernmt.persistence.Database;
import eu.modernmt.persistence.PersistenceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 20/04/16.
 */
public class ModernMT {

    private static final String BUILD_VERSION = Pom.getProperty("mmt.version");
    private static final long BUILD_NUMBER = Long.parseLong(Pom.getProperty("mmt.build.number"));

    private static final Logger logger = LogManager.getLogger(ModernMT.class);
    private static ClusterNode node = null;

    public static ClusterNode getNode() {
        if (node == null)
            throw new IllegalStateException("ModernMT node not available. You must must call runForever() first.");

        return node;
    }

    public static final TranslationFacade translation = new TranslationFacade();
    public static final MemoryFacade memory = new MemoryFacade();
    public static final TagFacade tags = new TagFacade();
    public static final TrainingFacade training = new TrainingFacade();

    public static void start(NodeConfig config, ClusterNode.StatusListener listener) throws FailedToJoinClusterException, BootstrapException {
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> logger.fatal("Unexpected exception thrown by thread [" + t.getName() + "]", e)
        );

        node = new ClusterNode("mmt-" + BUILD_VERSION + "-" + BUILD_NUMBER);
        if (listener != null)
            node.addStatusListener(listener);

        node.start(config, 30, TimeUnit.SECONDS);
    }

    public static ServerInfo info(boolean localhostOnly) {
        Engine engine = null;

        try {
            engine = node.getEngine();
        } catch (IllegalStateException e) {
            // Engine is not yet loaded
        }

        Collection<NodeInfo> nodes = localhostOnly ? Collections.singleton(node.getLocalNode()) : node.getClusterNodes();

        int memorySize = 0;
        if (engine != null) {
            try {
                Decoder decoder = engine.getDecoder();
                TranslationMemory memory = decoder.getTranslationMemory();
                memorySize = memory.size();
            } catch (UnsupportedOperationException e) {
                // Ignore - decoder not available
            }
        }

        return new ServerInfo(new ServerInfo.ClusterInfo(nodes), new ServerInfo.BuildInfo(BUILD_VERSION, BUILD_NUMBER), memorySize);
    }

    public static void test(boolean strict) throws TestFailedException {
        ClusterNode node = getNode();

        // 1 - Testing node status
        ClusterNode.Status status = node.getStatus();
        if (!ClusterNode.Status.RUNNING.equals(status) &&
                (strict || !ClusterNode.Status.DEGRADED.equals(status))) {
            throw new TestFailedException("Invalid node status: " + status);
        }

        // 2 - Testing decoder
        try {
            Decoder decoder = node.getEngine().getDecoder();
            decoder.test();
        } catch (DecoderException e) {
            throw new TestFailedException("Decoder test failed", e);
        } catch (UnsupportedOperationException e) {
            // Ignore - decoder not available
        }

        // 3 - Testing database connection
        try {
            Database db = node.getDatabase();
            db.testConnection();
        } catch (PersistenceException e) {
            throw new TestFailedException("Failed to connect to database", e);
        }
    }


}
