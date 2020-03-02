package eu.modernmt.cluster.kafka;

import eu.modernmt.data.LogChannel;
import org.apache.kafka.common.TopicPartition;

/**
 * Created by davide on 25/12/16.
 * Updated by andrearossi on 29/03/17
 * Wrapper for TopicPartition: it is used to assign an id to a TopicPartition
 */
public class KafkaChannel implements LogChannel {

    private final short id;
    private final TopicPartition partition;

    /**
     * It initializes a new TopicPartition
     * with the given topic name and partition number 0
     * <p>
     * NOTE: In a TopicPartition, we ALWAYS use partition 0 only
     *
     * @param id   the id for the new TopicPartition
     * @param name the topic name for the new TopicPartition
     */
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
