package eu.modernmt.network.messaging.zeromq;

/**
 * Created by davide on 25/11/15.
 */
public class ZMQMessagingConstants {

    public static final byte[] HEARTBEAT_PACKET = {0x01};
    public static final byte[] READY_PACKET = {0x01, 0x00};
    public static final byte[] PACKET_DIVIDER = new byte[0];

}
