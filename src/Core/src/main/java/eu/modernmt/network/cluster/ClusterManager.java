package eu.modernmt.network.cluster;

import eu.modernmt.network.messaging.MessagingServer;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.*;

/**
 * Created by davide on 19/11/15.
 */
public abstract class ClusterManager {

    public static final long DEFAULT_EXEC_SIGNAL_MIN_INTERVAL = 200;

    public static final byte SIGNAL_EXEC = 0x00;

    public static final byte REQUEST_EXEC = 0x00;
    public static final byte REQUEST_CALLB = 0x01;

    protected final Logger logger = LogManager.getLogger(getClass());

    private MessagingServer messagingServer;
    private ExecutionQueue executionQueue;

    private ExecDispatcher execDispatcher;
    private CallbDispatcher callbDispatcher;
    private Terminator terminator;

    public ClusterManager(MessagingServer messagingServer) {
        this(messagingServer, DEFAULT_EXEC_SIGNAL_MIN_INTERVAL);
    }

    public ClusterManager(MessagingServer messagingServer, long execSignalMinInterval) {
        this.executionQueue = new ExecutionQueue();
        this.messagingServer = messagingServer;
        this.messagingServer.setListener(new ClusterPacketListener());
        this.execDispatcher = new ExecDispatcher(execSignalMinInterval);
        this.callbDispatcher = new CallbDispatcher();
    }

    public void start() throws IOException {
        this.messagingServer.bind();
        this.callbDispatcher.start();
        this.execDispatcher.start();
    }

    public void shutdown() {
        this.executionQueue.shutdown();
        this.startTerminator();
    }

    private synchronized void startTerminator() {
        if (this.terminator == null) {
            this.terminator = new Terminator();
            this.terminator.start();
        }
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (this.terminator != null)
            this.terminator.awaitTermination(timeout, unit);
    }

    public void sendBroadcastSignal(byte signal, byte[] payload) throws IOException {
        if (signal == SIGNAL_EXEC)
            throw new IllegalArgumentException("Signal 0x00 is reserved for SIGNAL_EXEC");

        byte[] buffer = new byte[1 + (payload == null ? 0 : payload.length)];
        buffer[0] = signal;

        if (payload != null && payload.length > 0)
            System.arraycopy(payload, 0, buffer, 1, payload.length);

        internalSendBroadcastSignal(buffer);
    }

    private synchronized void internalSendBroadcastSignal(byte[] signal) throws IOException {
        this.messagingServer.sendBroadcastSignal(signal);
    }

    protected <V extends Serializable> Future<V> submit(DistributedCallable<V> callable) {
        DistributedTask<V> task = new DistributedTask<>(this.executionQueue, callable);
        this.executionQueue.add(task);
        return task;
    }

    protected abstract byte[] onCustomRequestReceived(byte signal, byte[] payload, int offset, int length);

    private class ExecDispatcher extends Thread {

        private final byte[] EXEC_PAYLOAD = {SIGNAL_EXEC};

        private boolean terminated;
        private long interval;

        public ExecDispatcher(long interval) {
            this.interval = interval;
            this.terminated = false;
        }

        public void terminate() {
            this.terminated = true;
            this.interrupt();
        }

        @Override
        public void run() {
            while (!this.isInterrupted() && !terminated) {
                if (executionQueue.size() > 0) {
                    try {
                        internalSendBroadcastSignal(EXEC_PAYLOAD);
                    } catch (IOException e) {
                        logger.error("Failed to send EXEC signal", e);
                    }
                }

                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    // Nothing to do
                }
            }
        }
    }

    private class CallbDispatcher extends Thread {

        private final CallableResponse POISON_PILL = CallableResponse.fromResult(null, null);
        private BlockingQueue<CallableResponse> queue = new LinkedBlockingQueue<>();

        public void schedule(CallableResponse response) {
            queue.add(response);
        }

        public void terminate() {
            this.queue.clear();
            this.queue.add(POISON_PILL);
            this.interrupt();
        }

        @Override
        public void run() {
            while (!this.isInterrupted()) {
                CallableResponse response = null;

                try {
                    response = queue.take();

                    if (response == POISON_PILL)
                        response = null;
                } catch (InterruptedException e) {
                    // Nothing to do;
                }

                if (response == null)
                    break;

                DistributedTask<?> task = executionQueue.removeRunningTask(response.id);

                if (task != null) {
                    if (response.hasError())
                        task.setException(response.throwable);
                    else
                        task.set(response.outcome);
                }
            }
        }
    }

    private class Terminator extends Thread {

        private CountDownLatch termination = new CountDownLatch(1);

        public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            this.termination.await(timeout, unit);
        }

        @Override
        public void run() {
            try {
                try {
                    execDispatcher.terminate();
                    execDispatcher.join();
                } catch (Throwable e) {
                    // Nothing to do
                }

                try {
                    callbDispatcher.terminate();
                    callbDispatcher.join();
                } catch (Throwable e) {
                    // Nothing to do
                }

                try {
                    messagingServer.shutdown();
                    messagingServer.awaitTermination();
                } catch (Throwable e) {
                    // Nothing to do
                }
            } finally {
                termination.countDown();
            }
        }
    }

    private class ClusterPacketListener implements MessagingServer.Listener {

        private byte[] encodeCallableRequests(int availability) {
            int size = 0;

            DistributedTask<?>[] tasks = new DistributedTask[availability];

            for (int i = 0; i < availability; i++) {
                tasks[i] = executionQueue.next();

                if (tasks[i] == null)
                    break;

                size++;
            }

            if (size > 0) {
                CallableRequest[] requests = new CallableRequest[size];
                for (int i = 0; i < size; i++)
                    requests[i] = new CallableRequest(tasks[i]);
                return SerializationUtils.serialize(requests);
            } else {
                return new byte[0];
            }
        }

        private byte[] onExecRequest(byte[] payload) {
            int availability = payload.length > 1 ? (payload[1] & 0xFF) : 1;
            return encodeCallableRequests(availability);
        }

        private byte[] onCallbackRequest(byte[] payload) {
            ByteArrayInputStream content = new ByteArrayInputStream(payload, 1, payload.length - 1);
            CallableResponse response = SerializationUtils.deserialize(content);
            callbDispatcher.schedule(response);

            return encodeCallableRequests(1);
        }

        @Override
        public byte[] onRequestReceived(byte[] payload) {
            byte signal = payload[0];

            switch (signal) {
                case REQUEST_EXEC:
                    return onExecRequest(payload);
                case REQUEST_CALLB:
                    return onCallbackRequest(payload);
                default:
                    return onCustomRequestReceived(signal, payload, 1, payload.length - 1);
            }
        }

    }

}
