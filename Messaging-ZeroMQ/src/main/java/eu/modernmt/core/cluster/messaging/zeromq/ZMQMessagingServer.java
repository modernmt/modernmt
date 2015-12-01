package eu.modernmt.core.cluster.messaging.zeromq;

import eu.modernmt.messaging.Message;
import eu.modernmt.messaging.MessagingService;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 24/11/15.
 */
public class ZMQMessagingServer implements MessagingService {

    private int port;
    private ZMQServerLoop mainLoop;
    Listener listener;

    public ZMQMessagingServer(int port) {
        this.port = port;
    }

    @Override
    public void open() throws IOException {
        this.mainLoop = new ZMQServerLoop(this, port);
        this.mainLoop.start();
    }

    private void ensureOpen() {
        if (mainLoop == null || mainLoop.isTerminated())
            throw new IllegalStateException("Messaging server is closed of has never been opened");
    }

    @Override
    public void open(UUID identity) throws IOException {
        throw new UnsupportedOperationException("Messaging server with ID not supported");
    }

    @Override
    public void sendAnycastMessage(Message message) throws IOException {
        ensureOpen();
        mainLoop.sendAnycastMessage(message);
    }

    @Override
    public void sendBroadcastMessage(Message message) throws IOException {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void shutdown() {
        mainLoop.shutdown();
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        mainLoop.join();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        unit.timedJoin(mainLoop, timeout);
        return !mainLoop.isAlive();
    }

}
