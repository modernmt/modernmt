package eu.modernmt.core.cluster.messaging.zeromq;

import eu.modernmt.util.UUIDUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;

import java.util.UUID;

/**
 * Created by davide on 25/11/15.
 */
public class ZMQClientLoop extends Thread implements AutoCloseable {

    private static int threadId = 0;

    private final Logger logger = LogManager.getLogger(ZMQClientLoop.class);

    private ZMQ.Context zmqContext;
    private ZMQ.Socket pubShutdownControlSocket;
    private ZMQ.Socket subShutdownControlSocket;
    private ZMQ.Socket anycastSocket;

    private ZMQMessagingClient parent;

    public ZMQClientLoop(ZMQMessagingClient parent, byte[] id, String serverHostname, int port) {
        super(ZMQClientLoop.class.getSimpleName() + "-" + ++threadId);
        this.parent = parent;
        this.zmqContext = ZMQ.context(1);

        String identity = getIdentity();
        String shutdownEndpoint = identity + '.' + "shutdown";

        this.pubShutdownControlSocket = zmqContext.socket(ZMQ.PAIR);
        this.pubShutdownControlSocket.bind("inproc://" + shutdownEndpoint);
        this.subShutdownControlSocket = zmqContext.socket(ZMQ.PAIR);
        this.subShutdownControlSocket.connect("inproc://" + shutdownEndpoint);

        this.anycastSocket = zmqContext.socket(ZMQ.REQ);
        this.anycastSocket.setIdentity(id);
        this.anycastSocket.connect(String.format("tcp://%s:%d", serverHostname, port));
    }

    private String getIdentity() {
        return getClass().getCanonicalName() + "@" + Integer.toHexString(super.hashCode());
    }

    public void shutdown() {
        pubShutdownControlSocket.send(ZMQMessagingConstants.HEARTBEAT_PACKET, 0);
    }

    @Override
    public void run() {
        anycastSocket.send(ZMQMessagingConstants.READY_PACKET, 0);

        while (!this.isInterrupted()) {
            ZMQ.Poller items = new ZMQ.Poller(2);

            items.register(subShutdownControlSocket, ZMQ.Poller.POLLIN);
            items.register(anycastSocket, ZMQ.Poller.POLLIN);

            if (items.poll() < 0) {
                logger.fatal("Failed to poll sockets");
                break;
            }

            if (items.pollin(0)) {
                logger.debug("Received shutdown request");
                break;
            } else if (items.pollin(1)) {
                byte[] messageId = anycastSocket.recv();
                byte[] divider = anycastSocket.recv();

                if (divider.length > 0)
                    continue;

                UUID messageUUID = UUIDUtils.parse(messageId);

                byte[] payload = anycastSocket.recv();
                logger.debug("Received " + payload.length + " bytes for message " + messageUUID);

                byte[] response;

                if (parent.listener == null) {
                    response = new byte[0];
                } else {
                    response = parent.listener.onAnycastMessageReceived(messageId, payload);
                }

                anycastSocket.send(messageId, 2);
                anycastSocket.send(ZMQMessagingConstants.PACKET_DIVIDER, 2);
                anycastSocket.send(response, 0);

                logger.debug("Sent " + response.length + " bytes for message " + messageUUID);
            }
        }

        close();
    }

    @Override
    public void close() {
        anycastSocket.close();
        subShutdownControlSocket.close();
        pubShutdownControlSocket.close();
        zmqContext.close();
    }
}
