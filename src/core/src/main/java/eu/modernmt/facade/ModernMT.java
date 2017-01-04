package eu.modernmt.facade;

import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.error.FailedToJoinClusterException;
import eu.modernmt.config.NodeConfig;
import eu.modernmt.engine.BootstrapException;
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

    public static void start(NodeConfig config, ClusterNode.StatusListener listener) throws FailedToJoinClusterException, BootstrapException {
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> logger.fatal("Unexpected exception thrown by thread [" + t.getName() + "]", e)
        );

        node = new ClusterNode();
        if (listener != null)
            node.addStatusListener(listener);

        node.start(config, 30, TimeUnit.SECONDS);
    }

}
