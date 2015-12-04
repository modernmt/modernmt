package eu.modernmt.network.cluster;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Created by davide on 19/11/15.
 */
public interface DistributedCallable<V extends Serializable> extends Serializable, Callable<V> {

}
