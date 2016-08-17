package eu.modernmt.facade;

import eu.modernmt.cluster.ClusterNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by davide on 20/04/16.
 */
public class ModernMT {

    private static final Logger logger = LogManager.getLogger(ModernMT.class);

    static {
        try {
            System.loadLibrary("mmtcore");
            logger.info("Library mmtcore loaded successfully");
        } catch (Throwable e) {
            logger.error("Unable to load library mmtcore", e);
            throw e;
        }
    }

    static ClusterNode node;

    public static void setLocalNode(ClusterNode node) {
        ModernMT.node = node;
    }

    public static final DecoderFacade decoder = new DecoderFacade();
    public static final ContextAnalyzerFacade context = new ContextAnalyzerFacade();
    public static final TagFacade tags = new TagFacade();
    public static final TrainingFacade training = new TrainingFacade();

}
