package eu.modernmt.core.cluster;

import eu.modernmt.messaging.Message;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

/**
 * Created by davide on 20/11/15.
 */
public class CallableResponse implements Message {

    public final byte[] id;
    public final Object outcome;
    public final Throwable throwable;

    public static CallableResponse parse(byte[] id, byte[] payload) {
        boolean success = payload[0] > 0;
        Object content = SerializationUtils.deserialize(new ByteArrayInputStream(payload, 1, payload.length - 1));

        return success ? new CallableResponse(id, content) : new CallableResponse((Throwable) content, id);
    }

    public CallableResponse(byte[] id, Object outcome) {
        this.id = id;
        this.outcome = outcome;
        this.throwable = null;
    }

    public CallableResponse(Throwable throwable, byte[] id) {
        this.id = id;
        this.outcome = null;
        this.throwable = throwable;
    }

    public boolean hasError() {
        return this.throwable != null;
    }

    @Override
    public byte[] getId() {
        return id;
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        stream.write(this.throwable != null ? 0x00 : 0x01);
        SerializationUtils.serialize(this.throwable != null ? this.throwable : (Serializable) this.outcome, stream);
        return stream.toByteArray();
    }
}
