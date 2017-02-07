package eu.modernmt.cluster.kafka;

import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * Created by davide on 30/08/16.
 */
public class KafkaElementSerializer implements Serializer<KafkaElement> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No options
    }

    @Override
    public byte[] serialize(String topic, KafkaElement data) {
        if (data == null)
            return null;

        return data.toBytes();
    }

    @Override
    public void close() {
        // Nothing to do
    }

}
