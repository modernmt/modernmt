package eu.modernmt.core.cluster.executor;

import java.io.Serializable;

/**
 * Created by davide on 20/04/16.
 */
class TaskOutcome implements Serializable {

    public final long id;
    public final Object value;
    public final Throwable exception;

    public TaskOutcome(long id, Object value) {
        this.id = id;
        this.value = value;
        this.exception = null;
    }

    public TaskOutcome(Throwable exception, long id) {
        this.id = id;
        this.value = null;
        this.exception = exception;
    }

}
