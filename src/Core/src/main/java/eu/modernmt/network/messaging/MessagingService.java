package eu.modernmt.network.messaging;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 23/11/15.
 */
public interface MessagingService {

    interface Listener {

        byte[] onAnycastMessageReceived(byte[] id, byte[] payload);

        byte[] onBroadcastMessageReceived(byte[] id, byte[] payload);

    }

    void open() throws IOException;

    void open(UUID identity) throws IOException;

    void sendAnycastMessage(Message message) throws IOException;

    void sendBroadcastMessage(Message message) throws IOException;

    void setListener(Listener listener);

    void shutdown();

    void awaitTermination() throws InterruptedException;

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

}
