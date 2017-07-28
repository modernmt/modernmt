package eu.modernmt.cluster.kafka;

import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Created by davide on 30/08/16.
 */
public class KafkaPacketSerializer implements Serializer<KafkaPacket> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No options
    }

    @Override
    public byte[] serialize(String topic, KafkaPacket data) {
        if (data == null)
            return null;

        return data.toBytes();
    }

    @Override
    public void close() {
        // Nothing to do
    }

}
