package eu.modernmt.core.cluster.messaging.zeromq;

import eu.modernmt.messaging.Message;
import eu.modernmt.messaging.MessagingService;
import eu.modernmt.util.UUIDUtils;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 25/11/15.
 */
public class ZMQMessagingClient implements MessagingService {

    private ZMQClientLoop mainLoop;
    private String serverHost;
    private int serverPort;

    Listener listener;

    public ZMQMessagingClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    @Override
    public void open() throws IOException {
        throw new UnsupportedOperationException("ZMQMessagingClient must be open with identity");
    }

    @Override
    public void open(UUID identity) throws IOException {
        this.mainLoop = new ZMQClientLoop(this, UUIDUtils.getBytes(identity), serverHost, serverPort);
        this.mainLoop.start();
    }

    @Override
    public void sendAnycastMessage(Message message) throws IOException {
        throw new UnsupportedOperationException("Sending from messaging client not permitted");
    }

    @Override
    public void sendBroadcastMessage(Message message) throws IOException {
        throw new UnsupportedOperationException("Sending from messaging client not permitted");
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
