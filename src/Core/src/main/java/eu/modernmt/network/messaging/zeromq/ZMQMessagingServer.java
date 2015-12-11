package eu.modernmt.network.messaging.zeromq;

import eu.modernmt.network.messaging.MessagingServer;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 24/11/15.
 */
public class ZMQMessagingServer implements MessagingServer {

    private int signalingSocketPort;
    private int dataSocketPort;

    private ServerLoop loop;
    protected Listener listener;

    public ZMQMessagingServer(int signalingSocketPort, int dataSocketPort) {
        this.signalingSocketPort = signalingSocketPort;
        this.dataSocketPort = dataSocketPort;
    }

    @Override
    public void bind() throws IOException {
        this.loop = new ServerLoop(signalingSocketPort, dataSocketPort);
        this.loop.start();
    }

    @Override
    public void sendBroadcastSignal(byte[] signal) throws IOException {
        if (loop == null || loop.isTerminated())
            throw new IllegalStateException("Messaging server is closed of has never been opened");

        loop.sendBroadcastSignal(signal);
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

    private class ServerLoop extends ZMQAbstractLoop {

        public ServerLoop(int signalingSocketPort, int dataSocketPort) {
            super(signalingSocketPort, dataSocketPort);
        }

        public void sendBroadcastSignal(byte[] signal) {
            signalingSocket.send(signal, 0);
        }

        @Override
        protected void onDataAvailable(ZMQ.Socket socket) {
            byte[] id = socket.recv();
            byte[] divider = socket.recv();

            if (divider.length > 0)
                return;

            byte[] payload = socket.recv();
            byte[] response;

            Listener listener = ZMQMessagingServer.this.listener;
            response = listener == null ? new byte[0] : listener.onRequestReceived(payload);

            socket.send(id, 2);
            socket.send(divider, 2);
            socket.send(response, 0);
        }

    }

}
