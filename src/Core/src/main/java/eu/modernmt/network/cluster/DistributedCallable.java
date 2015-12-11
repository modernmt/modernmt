package eu.modernmt.network.cluster;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Created by davide on 19/11/15.
 */
public abstract class DistributedCallable<V extends Serializable> implements Serializable, Callable<V> {

    private transient Worker worker;

    void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Worker getWorker() {
        return worker;
    }

}
