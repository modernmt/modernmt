package eu.modernmt.network.cluster;

import eu.modernmt.network.messaging.MessagingClient;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 20/11/15.
 */
public abstract class Worker {

    private final Logger logger = LogManager.getLogger(Worker.class);

    private MessagingClient messagingClient;
    private boolean ready;
    private WorkerExecutor executor;

    public Worker(MessagingClient messagingClient, int capacity) {
        this.messagingClient = messagingClient;
        this.messagingClient.setListener(new WorkerPacketListener());
        this.ready = false;
        this.executor = new WorkerExecutor(this, capacity);
    }

    public void start() throws IOException {
        this.messagingClient.connect();
    }

    protected void ready() {
        this.ready = true;
    }

    public void shutdown() {
        this.messagingClient.shutdown();
        this.executor.shutdown();
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        this.messagingClient.awaitTermination(timeout, unit);
        this.executor.awaitTermination(timeout, unit);
    }

    protected abstract void onCustomBroadcastSignalReceived(byte signal, byte[] payload, int offset, int length);

    public byte[] sendRequest(byte type, byte[] payload, TimeUnit unit, long timeout) throws IOException, InterruptedException {
        if (type <= Cluster.REQUEST_CALLB)
            throw new IllegalArgumentException("Signal " + Integer.toHexString(type) + " is reserved.");

        byte[] buffer = new byte[1 + (payload == null ? 0 : payload.length)];
        buffer[0] = type;

        if (payload != null && payload.length > 0)
            System.arraycopy(payload, 0, buffer, 1, payload.length);

        return internalSendRequest(buffer, unit, timeout);
    }

    void sendResponse(CallableResponse response) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(Cluster.REQUEST_CALLB);
        SerializationUtils.serialize(response, stream);

        try {
            internalSendRequest(stream.toByteArray(), TimeUnit.MINUTES, 1);
        } catch (IOException | InterruptedException e) {
            logger.debug("Worker could not send response back.", e);
        }
    }

    private synchronized byte[] internalSendRequest(byte[] payload, TimeUnit unit, long timeout) throws IOException, InterruptedException {
        return this.messagingClient.request(payload, unit, timeout);
    }

    private class WorkerPacketListener implements MessagingClient.Listener {

        @Override
        public void onBroadcastSignalReceived(byte[] payload) {
            byte signal = payload[0];

            if (signal == Cluster.SIGNAL_EXEC) {
                if (!ready) {
                    logger.debug("Worker not ready, ignoring SIGNAL_EXEC.");
                    return;
                }

                if (executor.getAvailability() == 0) {
                    logger.debug("Worker busy, ignoring SIGNAL_EXEC.");
                    return;
                }

                byte[] job = null;
                try {
                    job = internalSendRequest(new byte[]{Cluster.REQUEST_EXEC}, TimeUnit.MINUTES, 1);
                } catch (IOException | InterruptedException e) {
                    // Nothing to do
                }

                if (job != null && job.length > 0) {
                    CallableRequest[] requests = SerializationUtils.deserialize(job);
                    executor.submit(requests);
                }
            } else {
                onCustomBroadcastSignalReceived(signal, payload, 1, payload.length - 1);
            }
        }

    }
}
