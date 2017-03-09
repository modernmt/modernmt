package eu.modernmt.data;

/**
 * Created by davide on 06/09/16.
 */
public class Deletion extends DataMessage {

    public final int domain;

    public Deletion(short channel, long channelPosition, int domain) {
        super(channel, channelPosition);
        this.domain = domain;
    }

}
