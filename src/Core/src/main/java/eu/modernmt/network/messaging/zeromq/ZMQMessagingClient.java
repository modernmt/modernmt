package eu.modernmt.network.messaging.zeromq;

import eu.modernmt.network.messaging.MessagingClient;
import org.zeromq.ZMQ;
import zmq.ZError;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 25/11/15.
 */
public class ZMQMessagingClient implements MessagingClient {

    private String serverHost;
    private int signalingSocketPort;
    private int dataSocketPort;

    private ClientLoop loop;
    protected Listener listener;

    public ZMQMessagingClient(String serverHost, int signalingSocketPort, int dataSocketPort) {
        this.serverHost = serverHost;
        this.signalingSocketPort = signalingSocketPort;
        this.dataSocketPort = dataSocketPort;
    }

    @Override
    public void connect() throws IOException {
        this.loop = new ClientLoop(serverHost, signalingSocketPort, dataSocketPort);
        this.loop.start();
    }

    @Override
    public byte[] request(byte[] payload, TimeUnit unit, long timeout) throws IOException, InterruptedException {
        if (loop == null || loop.isTerminated())
            throw new IllegalStateException("Messaging client is closed of has never been opened");


        return loop.request(payload, unit, timeout);
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void shutdown() {
        this.loop.shutdown();
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        this.loop.join();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        unit.timedJoin(loop, timeout);
        return !loop.isAlive();
    }

    private class ClientLoop extends ZMQAbstractLoop {

        public ClientLoop(String serverHost, int signalingSocketPort, int dataSocketPort) {
            super(serverHost, signalingSocketPort, dataSocketPort);
        }

        public byte[] request(byte[] payload, TimeUnit unit, long timeout) throws InterruptedException {
            dataSocket.send(payload, 0);

            ZMQ.Poller items = new ZMQ.Poller(2);

            items.register(subShutdownSocket, ZMQ.Poller.POLLIN);
            items.register(dataSocket, ZMQ.Poller.POLLIN);

            try {
                if (items.poll(unit.toMillis(timeout)) == 0) {
                    throw new InterruptedException();
                } else {
                    if (items.pollin(1)) {
                        return dataSocket.recv();
                    } else {
                        throw new InterruptedException();
                    }
                }
            } catch (ZError.IOException e) {
                Throwable cause = e.getCause();

                if (cause instanceof ClosedChannelException) {
                    throw new InterruptedException();
                } else {
                    throw e;
                }
            }
        }

        @Override
        protected void onDataAvailable(ZMQ.Socket socket) {
            byte[] payload = socket.recv();

            Listener listener = ZMQMessagingClient.this.listener;
            if (listener != null) {
                listener.onBroadcastSignalReceived(payload);
            }
        }

    }

}
