package eu.modernmt.rest.model;

import eu.modernmt.cluster.NodeInfo;

import java.util.Collection;

/**
 * Created by davide on 15/12/16.
 */
public class ServerStatistics {

    public static class ClusterStats {

        private final Collection<NodeInfo> nodes;

        public ClusterStats(Collection<NodeInfo> nodes) {
            this.nodes = nodes;
        }
    }

    private final ClusterStats cluster;

    public ServerStatistics(ClusterStats cluster) {
        this.cluster = cluster;
    }

}
