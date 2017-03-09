package eu.modernmt.data;

/**
 * Created by davide on 07/02/17.
 */
public abstract class DataMessage {

    public final short channel;
    public final long channelPosition;

    public DataMessage(short channel, long channelPosition) {
        this.channel = channel;
        this.channelPosition = channelPosition;
    }

}
