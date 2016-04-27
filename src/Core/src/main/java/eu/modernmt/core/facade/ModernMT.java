package eu.modernmt.core.facade;

import eu.modernmt.core.cluster.ClusterNode;

/**
 * Created by davide on 20/04/16.
 */
public class ModernMT {

    protected static ClusterNode node;

    public static void setLocalNode(ClusterNode node) {
        ModernMT.node = node;
    }

    public static final DecoderFacade decoder = new DecoderFacade();
    public static final ContextAnalyzerFacade context = new ContextAnalyzerFacade();
    public static final TagFacade tags = new TagFacade();

}
