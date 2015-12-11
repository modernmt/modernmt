package eu.modernmt.network.cluster;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by davide on 20/11/15.
 */
public class CallableRequest implements Serializable {

    public final UUID id;
    public final DistributedCallable<?> callable;

    public CallableRequest(DistributedTask<?> task) {
        this.id = task.getId();
        this.callable = task.getCallable();
    }

}
