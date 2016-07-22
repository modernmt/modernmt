package eu.modernmt.facade.operations;

import eu.modernmt.Engine;
import eu.modernmt.cluster.executor.DistributedCallable;

import java.io.Serializable;

/**
 * Created by davide on 22/04/16.
 */
public abstract class Operation<V extends Serializable> extends DistributedCallable<V> {

    protected Engine getEngine() {
        return getLocalNode().getEngine();
    }

}
