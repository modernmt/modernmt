package eu.modernmt.facade;

import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.error.BootstrapException;
import eu.modernmt.cluster.error.FailedToJoinClusterException;
import eu.modernmt.engine.Engine;
import eu.modernmt.engine.config.EngineConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 20/04/16.
 */
public class ModernMT {

    private static final Logger logger = LogManager.getLogger(ModernMT.class);
    private static ClusterNode node = null;

    static ClusterNode getNode() {
        if (node == null)
            throw new IllegalStateException("ModernMT node not available. You must must call start() first.");

        return node;
    }

    public static final DecoderFacade decoder = new DecoderFacade();
    public static final ContextAnalyzerFacade context = new ContextAnalyzerFacade();
    public static final TagFacade tags = new TagFacade();
    public static final TrainingFacade training = new TrainingFacade();
    public static final DomainFacade domain = new DomainFacade();
    public static final EngineFacade engine = new EngineFacade();
    public static final ClusterFacade cluster = new ClusterFacade();

    public static class ClusterOptions {

        public int controlPort = 5016;
        public int dataPort = 5017;
        public String member = null;

        public ClusterNode.StatusListener statusListener = null;

    }

    public static void start(ClusterOptions options, EngineConfig engineConfig) throws FailedToJoinClusterException, BootstrapException {
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> logger.fatal("Unexpected exception thrown by thread [" + t.getName() + "]", e)
        );

        Engine engine = new Engine(engineConfig);
        node = new ClusterNode(engine, options.controlPort, options.dataPort);

        if (options.statusListener != null)
            node.addStatusListener(options.statusListener);

        if (options.member != null)
            node.joinCluster(options.member, 30, TimeUnit.SECONDS);
        else
            node.startCluster();
    }

}
