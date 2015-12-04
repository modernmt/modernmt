package eu.modernmt.network.cluster;

import eu.modernmt.network.messaging.Message;
import eu.modernmt.network.uuid.UUIDUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * Created by davide on 20/11/15.
 */
public class CallableRequest implements Message {

    public final byte[] id;
    public final DistributedCallable<?> callable;

    public static CallableRequest parse(byte[] id, byte[] payload) {
        DistributedCallable<?> callable = SerializationUtils.deserialize(new ByteArrayInputStream(payload));

        return new CallableRequest(id, callable);
    }

    public CallableRequest(UUID id, DistributedCallable<?> callable) {
        this(UUIDUtils.getBytes(id), callable);
    }

    public CallableRequest(byte[] id, DistributedCallable<?> callable) {
        this.id = id;
        this.callable = callable;
    }

    @Override
    public byte[] getId() {
        return id;
    }

    @Override
    public byte[] getBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        SerializationUtils.serialize(this.callable, stream);
        return stream.toByteArray();
    }

}
