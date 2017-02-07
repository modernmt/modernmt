package eu.modernmt.cluster.kafka;

import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * Created by davide on 30/08/16.
 */
public class KafkaElementDeserializer implements Deserializer<KafkaElement> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No options
    }

    @Override
    public KafkaElement deserialize(String topic, byte[] data) {
        if (data == null)
            return null;

        return KafkaElement.fromBytes(data);
    }

    @Override
    public void close() {
        // Nothing to do
    }

}
