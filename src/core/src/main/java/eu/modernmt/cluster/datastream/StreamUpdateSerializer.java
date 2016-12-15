package eu.modernmt.cluster.datastream;

import eu.modernmt.io.DefaultCharset;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Created by davide on 30/08/16.
 */
public class StreamUpdateSerializer implements Serializer<StreamUpdate> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // No options
    }

    @Override
    public byte[] serialize(String topic, StreamUpdate data) {
        if (data == null)
            return null;

        Charset charset = DefaultCharset.get();

        byte flags = 0; // Reserved for future use

        int domain = data.getDomain();
        byte[] sourceSentence = data.getSourceSentence().getBytes(charset);
        byte[] targetSentence = data.getTargetSentence().getBytes(charset);

        ByteBuffer buffer = ByteBuffer.allocate(13 + sourceSentence.length + targetSentence.length);
        buffer.put(flags);
        buffer.putInt(domain);
        buffer.putInt(sourceSentence.length);
        buffer.put(sourceSentence);
        buffer.putInt(targetSentence.length);
        buffer.put(targetSentence);

        return buffer.array();
    }

    @Override
    public void close() {

    }

}
