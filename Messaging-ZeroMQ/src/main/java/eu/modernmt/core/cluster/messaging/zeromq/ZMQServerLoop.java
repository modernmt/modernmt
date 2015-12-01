package eu.modernmt.core.cluster.messaging.zeromq;

import eu.modernmt.messaging.Message;
import eu.modernmt.util.UUIDUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMQ;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by davide on 24/11/15.
 */
public class ZMQServerLoop extends Thread implements AutoCloseable {

    private static int threadId = 0;

    private final Logger logger = LogManager.getLogger(ZMQServerLoop.class);

    private Set<UUID> busyWorkers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private BlockingDeque<UUID> idleWorkers = new LinkedBlockingDeque<>();
    private Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    private ZMQ.Context zmqContext;
    private ZMQ.Socket pubShutdownControlSocket;
    private ZMQ.Socket pubMessageControlSocket;
    private ZMQ.Socket subShutdownControlSocket;
    private ZMQ.Socket subMessageControlSocket;
    private ZMQ.Socket anycastSocket;
    private boolean terminated;

    private ZMQMessagingServer parent;

    public ZMQServerLoop(ZMQMessagingServer parent, int port) {
        super(ZMQServerLoop.class.getSimpleName() + "-" + ++threadId);
        this.parent = parent;
        this.terminated = false;
        this.zmqContext = ZMQ.context(1);

        String identity = getIdentity();
        String shutdownEndpoint = identity + '.' + "shutdown";
        String messageEndpoint = identity + '.' + "message";

        this.pubShutdownControlSocket = zmqContext.socket(ZMQ.PAIR);
        this.pubShutdownControlSocket.bind("inproc://" + shutdownEndpoint);
        this.subShutdownControlSocket = zmqContext.socket(ZMQ.PAIR);
        this.subShutdownControlSocket.connect("inproc://" + shutdownEndpoint);

        this.pubMessageControlSocket = zmqContext.socket(ZMQ.PAIR);
        this.pubMessageControlSocket.bind("inproc://" + messageEndpoint);
        this.subMessageControlSocket = zmqContext.socket(ZMQ.PAIR);
        this.subMessageControlSocket.connect("inproc://" + messageEndpoint);

        this.anycastSocket = zmqContext.socket(ZMQ.ROUTER);
        this.anycastSocket.bind(String.format("tcp://*:%d", port));
    }

    private String getIdentity() {
        return getClass().getCanonicalName() + "@" + Integer.toHexString(super.hashCode());
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void shutdown() {
        if (!terminated) {
            pubShutdownControlSocket.send(ZMQMessagingConstants.HEARTBEAT_PACKET, 0);
            terminated = true;
        }
    }

    public void sendAnycastMessage(Message message) {
        messageQueue.add(message);
        pubMessageControlSocket.send(ZMQMessagingConstants.HEARTBEAT_PACKET, 0);
    }

    @Override
    public void run() {
        while (!this.isInterrupted()) {
            ZMQ.Poller items = new ZMQ.Poller(3);

            items.register(subShutdownControlSocket, ZMQ.Poller.POLLIN);
            items.register(anycastSocket, ZMQ.Poller.POLLIN);

            if (!idleWorkers.isEmpty())
                items.register(subMessageControlSocket, ZMQ.Poller.POLLIN);

            if (items.poll() < 0) {
                logger.fatal("Failed to poll sockets");
                break;
            }

            if (items.pollin(0)) {
                logger.debug("Received shutdown request");
                break;
            } else if (items.pollin(1)) {
                byte[] workerId = anycastSocket.recv();
                byte[] divider = anycastSocket.recv();

                if (divider.length > 0)
                    continue;

                byte[] payload = anycastSocket.recv();

                UUID wuuid = UUIDUtils.parse(workerId);

                if (Arrays.equals(ZMQMessagingConstants.READY_PACKET, payload)) {
                    idleWorkers.add(wuuid);
                    logger.debug("Worker registered (" + wuuid + "), " + idleWorkers.size() + " available");
                } else {
                    byte[] messageId = payload;
                    divider = anycastSocket.recv();

                    if (divider.length > 0)
                        continue;

                    payload = anycastSocket.recv();
                    logger.debug("Received " + payload.length + " bytes for message " + UUIDUtils.parse(messageId));

                    if (parent.listener != null)
                        parent.listener.onAnycastMessageReceived(messageId, payload);

                    idleWorkers.add(wuuid);
                }
            } else if (items.pollin(2)) {
                Message message = messageQueue.poll();
                UUID worker = idleWorkers.poll();

                if (message == null || worker == null)
                    continue;

                busyWorkers.add(worker);

                byte[] messageId = message.getId();
                byte[] messagePayload = message.getBytes();

                anycastSocket.send(UUIDUtils.getBytes(worker), 2);
                anycastSocket.send(ZMQMessagingConstants.PACKET_DIVIDER, 2);
                anycastSocket.send(messageId, 2);
                anycastSocket.send(ZMQMessagingConstants.PACKET_DIVIDER, 2);
                anycastSocket.send(messagePayload, 0);

                logger.debug("Sent " + messagePayload.length + " bytes for message " + UUIDUtils.parse(messageId));
            }
        }

        close();
    }

    @Override
    public void close() {
        anycastSocket.close();
        subMessageControlSocket.close();
        subShutdownControlSocket.close();
        pubMessageControlSocket.close();
        pubShutdownControlSocket.close();
        zmqContext.close();
    }

}
