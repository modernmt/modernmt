package eu.modernmt.data;

import java.util.UUID;

/**
 * Created by davide on 06/09/16.
 */
public class DeletionMessage extends DataMessage {

    public final UUID owner;
    public final long memory;

    public DeletionMessage(short channel, long channelPosition, UUID owner, long memory) {
        super(channel, channelPosition);
        this.owner = owner;
        this.memory = memory;
    }

}
