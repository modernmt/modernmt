package eu.modernmt.cluster.kafka;

import eu.modernmt.io.DefaultCharset;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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

        Charset charset = DefaultCharset.get();

        ByteBuffer buffer = ByteBuffer.wrap(data);

        buffer.get(); // flags: reserved for future use

        int domain = buffer.getInt();

        int sourceSentenceLength = buffer.getInt();
        String sourceSentence = new String(data, 9, sourceSentenceLength, charset);
        buffer.position(9 + sourceSentenceLength);

        int targetSentenceLength = buffer.getInt();
        String targetSentence = new String(data, 13 + sourceSentenceLength, targetSentenceLength, charset);

        return new KafkaElement(domain, sourceSentence, targetSentence);
    }

    @Override
    public void close() {

    }

}
