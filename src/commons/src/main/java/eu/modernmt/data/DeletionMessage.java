package eu.modernmt.data;

/**
 * Created by davide on 06/09/16.
 */
public class DeletionMessage extends DataMessage {

    public final long memory;

    public DeletionMessage(short channel, long channelPosition, long memory) {
        super(channel, channelPosition);
        this.memory = memory;
    }

}
