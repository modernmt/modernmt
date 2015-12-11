package eu.modernmt.network.messaging;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 10/12/15.
 */
public interface MessagingServer {

    interface Listener {

        byte[] onRequestReceived(byte[] payload);

    }

    void bind() throws IOException;

    void sendBroadcastSignal(byte[] signal) throws IOException;

    void setListener(Listener listener);

    void shutdown();

    void awaitTermination() throws InterruptedException;

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

}
