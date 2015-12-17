package eu.modernmt.network.messaging.zeromq;

import eu.modernmt.network.uuid.UUIDSequence;
import eu.modernmt.network.uuid.UUIDUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import java.io.Closeable;
import java.util.UUID;

/**
 * Created by davide on 11/12/15.
 */
public abstract class ZMQAbstractLoop extends Thread implements Closeable, AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static int threadId = 0;

    private ZMQ.Context context;
    private ZMQ.Socket pubShutdownSocket;
    private ZMQ.Socket subShutdownSocket;
    protected ZMQ.Socket signalingSocket;
    protected ZMQ.Socket dataSocket;

    private ZMQ.Socket listeningSocket;

    private boolean terminated;

    public ZMQAbstractLoop(String serverHost, int signalingSocketPort, int dataSocketPort) {
        init();

        this.signalingSocket = context.socket(ZMQ.SUB);
        this.signalingSocket.setRcvHWM(0);
        this.signalingSocket.connect(String.format("tcp://%s:%d", serverHost, signalingSocketPort));
        this.signalingSocket.subscribe(new byte[0]);

        UUID uuid = UUIDSequence.next(UUIDSequence.SequenceType.MESSAGING_IDENTITY);

        this.dataSocket = context.socket(ZMQ.REQ);
        this.dataSocket.setIdentity(UUIDUtils.getBytes(uuid));
        this.dataSocket.connect(String.format("tcp://%s:%d", serverHost, dataSocketPort));

        this.listeningSocket = this.signalingSocket;
    }

    public ZMQAbstractLoop(int signalingSocketPort, int dataSocketPort) {
        init();

        this.signalingSocket = context.socket(ZMQ.PUB);
        this.signalingSocket.setLinger(5000);
        this.signalingSocket.setSndHWM(0);
        this.signalingSocket.bind(String.format("tcp://*:%d", signalingSocketPort));

        this.dataSocket = context.socket(ZMQ.ROUTER);
        this.dataSocket.bind(String.format("tcp://*:%d", dataSocketPort));

        this.listeningSocket = this.dataSocket;
    }

    private void init() {
        super.setName(getClass().getSimpleName() + "-" + ++threadId);
        this.terminated = false;

        String shutdownEndpoint = getInstanceIdentity() + ".shutdown";

        this.context = ZMQ.context(1);
        this.pubShutdownSocket = context.socket(ZMQ.PAIR);
        this.pubShutdownSocket.bind("inproc://" + shutdownEndpoint);
        this.subShutdownSocket = context.socket(ZMQ.PAIR);
        this.subShutdownSocket.connect("inproc://" + shutdownEndpoint);
    }

    private String getInstanceIdentity() {
        return getClass().getCanonicalName() + "@" + Integer.toHexString(super.hashCode());
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void shutdown() {
        if (!terminated) {
            terminated = true;
            pubShutdownSocket.send(new byte[]{0x00}, 0);
        }
    }

    protected abstract void onDataAvailable(ZMQ.Socket listeningSocket);

    @Override
    public void run() {
        while (!this.isInterrupted() && !terminated) {
            ZMQ.Poller items = new ZMQ.Poller(2);

            items.register(subShutdownSocket, ZMQ.Poller.POLLIN);
            items.register(listeningSocket, ZMQ.Poller.POLLIN);

            if (items.poll() < 0) {
                logger.error("Failed to poll sockets");
                break;
            }

            if (items.pollin(0)) {
                logger.debug("Received shutdown request");
                break;
            } else {
                onDataAvailable(listeningSocket);
            }
        }

        close();
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(pubShutdownSocket);
        IOUtils.closeQuietly(subShutdownSocket);
        IOUtils.closeQuietly(signalingSocket);
        IOUtils.closeQuietly(dataSocket);
        IOUtils.closeQuietly(context);
    }
}
