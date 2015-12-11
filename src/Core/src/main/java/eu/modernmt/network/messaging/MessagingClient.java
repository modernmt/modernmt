package eu.modernmt.network.messaging;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 10/12/15.
 */
public interface MessagingClient {

    interface Listener {

        void onBroadcastSignalReceived(byte[] signal);

    }

    void connect() throws IOException;

    byte[] request(byte[] payload, TimeUnit unit, long timeout) throws IOException, InterruptedException;

    void setListener(Listener listener);

    void shutdown();

    void awaitTermination() throws InterruptedException;

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

}
