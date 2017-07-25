package eu.modernmt.data;

/**
 * Created by davide on 06/09/16.
 */
public class Deletion extends DataMessage {

    public final long domain;

    public Deletion(short channel, long channelPosition, long domain) {
        super(channel, channelPosition);
        this.domain = domain;
    }

}
