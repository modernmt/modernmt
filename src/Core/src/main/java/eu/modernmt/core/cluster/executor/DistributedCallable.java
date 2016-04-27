package eu.modernmt.core.cluster.executor;

import eu.modernmt.core.cluster.ClusterNode;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Created by davide on 20/04/16.
 */
public abstract class DistributedCallable<V> implements Callable<V>, Serializable {

    private transient ClusterNode localNode;

    void setLocalNode(ClusterNode localNode) {
        this.localNode = localNode;
    }

    protected ClusterNode getLocalNode() {
        return localNode;
    }

}
