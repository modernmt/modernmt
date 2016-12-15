package eu.modernmt.facade;

import eu.modernmt.cluster.NodeInfo;

import java.util.Collection;

/**
 * Created by davide on 15/12/16.
 */
public class ClusterFacade {

    public Collection<NodeInfo> getNodes() {
        return ModernMT.getNode().getClusterNodes();
    }

}
