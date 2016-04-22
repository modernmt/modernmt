package eu.modernmt.core.facade.operations;

import eu.modernmt.core.Engine;
import eu.modernmt.core.cluster.executor.DistributedCallable;

import java.io.Serializable;

/**
 * Created by davide on 22/04/16.
 */
public abstract class Operation<V extends Serializable> extends DistributedCallable<V> {

    protected Engine getEngine() {
        return getLocalMember().getEngine();
    }

}
