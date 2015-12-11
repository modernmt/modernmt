package eu.modernmt.network.cluster;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by davide on 20/11/15.
 */
public class CallableResponse implements Serializable {

    public final UUID id;
    public final Object outcome;
    public final Throwable throwable;

    public static CallableResponse fromError(UUID id, Throwable throwable) {
        return new CallableResponse(throwable, id);
    }

    public static CallableResponse fromResult(UUID id, Object result) {
        return new CallableResponse(id, result);
    }

    public CallableResponse(UUID id, Object outcome) {
        this.id = id;
        this.outcome = outcome;
        this.throwable = null;
    }

    public CallableResponse(Throwable throwable, UUID id) {
        this.id = id;
        this.outcome = null;
        this.throwable = throwable;
    }

    public boolean hasError() {
        return this.throwable != null;
    }

}
