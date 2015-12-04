package eu.modernmt.network.cluster;

import eu.modernmt.network.messaging.MessagingService;
import eu.modernmt.network.uuid.UUIDSequence;
import eu.modernmt.network.uuid.UUIDUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 20/11/15.
 */
public class Worker {

    private final Logger logger = LogManager.getLogger(Worker.class);

    private UUID id;
    private MessagingService messagingService;

    public Worker(MessagingService messagingService) throws IOException {
        this.id = UUIDSequence.next(UUIDSequence.SequenceType.WORKER);
        this.messagingService = messagingService;
        this.messagingService.setListener(new WorkerPacketListener());
        this.messagingService.open();
    }

    public UUID getId() {
        return id;
    }

    protected CallableResponse execute(byte[] id, DistributedCallable<?> callable) {
        CallableResponse response;

        try {
            response = new CallableResponse(id, callable.call());
        } catch (Exception e) {
            response = new CallableResponse(id, e);
        }

        return response;
    }

    public void shutdown() {
        this.messagingService.shutdown();
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        this.messagingService.awaitTermination(timeout, unit);
    }

    protected class WorkerPacketListener implements MessagingService.Listener {

        @Override
        public byte[] onAnycastMessageReceived(byte[] id, byte[] payload) {
            CallableRequest request = CallableRequest.parse(id, payload);
            logger.info("Received CallableRequest with id " + UUIDUtils.parse(request.id));

            CallableResponse response = execute(request.id, request.callable);
            logger.info("Send response to CallableRequest with id " + UUIDUtils.parse(response.id) + ", hasError = " + response.hasError());

            return response.getBytes();
        }

        @Override
        public byte[] onBroadcastMessageReceived(byte[] id, byte[] payload) {
            // Not used right now
            return null;
        }

    }
}
