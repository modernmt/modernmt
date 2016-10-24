package eu.modernmt.facade;

import eu.modernmt.cluster.ClusterNode;

/**
 * Created by davide on 20/04/16.
 */
public class ModernMT {

    static ClusterNode node;

    public static void setLocalNode(ClusterNode node) {
        ModernMT.node = node;
    }

    public static final DecoderFacade decoder = new DecoderFacade();
    public static final ContextAnalyzerFacade context = new ContextAnalyzerFacade();
    public static final TagFacade tags = new TagFacade();
    public static final TrainingFacade training = new TrainingFacade();
    public static final DomainFacade domain = new DomainFacade();
    public static final EngineFacade engine = new EngineFacade();

}
