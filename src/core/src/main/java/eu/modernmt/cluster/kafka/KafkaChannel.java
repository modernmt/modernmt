package eu.modernmt.cluster.kafka;

import eu.modernmt.data.DataChannel;
import org.apache.kafka.common.TopicPartition;

/**
 * Created by davide on 25/12/16.
 */
public class KafkaChannel implements DataChannel {

    private final short id;
    private final TopicPartition partition;

    public KafkaChannel(int id, String name) {
        this.id = (short) id;
        this.partition = new TopicPartition(name, 0);
    }

    @Override
    public short getId() {
        return id;
    }

    @Override
    public String getName() {
        return partition.topic();
    }

    @Override
    public String toString() {
        return "KafkaChannel(" + getName() + ')';
    }

    public TopicPartition getTopicPartition() {
        return partition;
    }
}
