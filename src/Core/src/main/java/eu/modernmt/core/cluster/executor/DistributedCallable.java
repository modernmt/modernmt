package eu.modernmt.core.cluster.executor;

import eu.modernmt.core.cluster.Member;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Created by davide on 20/04/16.
 */
public abstract class DistributedCallable<V> implements Callable<V>, Serializable {

    private transient Member localMember;

    void setLocalMember(Member localMember) {
        this.localMember = localMember;
    }

    protected Member getLocalMember() {
        return localMember;
    }

}
