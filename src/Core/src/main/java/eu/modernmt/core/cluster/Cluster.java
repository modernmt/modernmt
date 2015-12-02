package eu.modernmt.core.cluster;

import eu.modernmt.messaging.MessagingService;
import eu.modernmt.util.UUIDUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 19/11/15.
 */
public class Cluster {

    private final Logger logger = LogManager.getLogger(Cluster.class);

    private Dispatcher dispatcher;
    private Terminator terminator;
    private ExecutionQueue executionQueue;
    private MessagingService messagingService;

    public Cluster(MessagingService messagingService) throws IOException {
        this.executionQueue = new ExecutionQueue();
        this.messagingService = messagingService;
        this.messagingService.setListener(new ClusterPacketListener());
        this.messagingService.open();
        this.dispatcher = new Dispatcher();
        this.dispatcher.start();
    }

    public boolean isShutdown() {
        return this.executionQueue.isShutdown();
    }

    public boolean isTerminated() {
        return this.executionQueue.isTerminated();
    }

    public void shutdown() {
        this.executionQueue.shutdown(false);
        this.startTerminator();
    }

    public List<DistributedTask<?>> shutdownNow() {
        List<DistributedTask<?>> pending = this.executionQueue.shutdown(true);
        this.startTerminator();

        return pending;
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

    void removeFromExecution(DistributedTask<?> task, boolean interruptIfRunning) {
        this.executionQueue.remove(task, interruptIfRunning);
    }

    void exec(DistributedTask<?> task) {
        CallableRequest request = new CallableRequest(task.getId(), task.getCallable());

        try {
            this.messagingService.sendAnycastMessage(request);
        } catch (IOException e) {
            task = executionQueue.removePendingTask(task.getId());
            if (task != null)
                task.setException(e);
        }
    }

    public <V extends Serializable> Future<V> submit(DistributedCallable<V> callable) {
        DistributedTask<V> task = new DistributedTask<>(this, callable);
        this.executionQueue.add(task);
        return task;
    }

    private class Dispatcher extends Thread {

        @Override
        public void run() {
            DistributedTask<?> task;

            while ((task = executionQueue.next()) != null) {
                task.run();
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
                    dispatcher.join();
                } catch (Throwable e) {
                    // Nothing to do
                }

                try {
                    executionQueue.awaitTermination();
                } catch (Throwable e) {
                    // Nothing to do
                }

                try {
                    messagingService.shutdown();
                    messagingService.awaitTermination();
                } catch (Throwable e) {
                    // Nothing to do
                }
            } finally {
                termination.countDown();
            }
        }
    }

    protected class ClusterPacketListener implements MessagingService.Listener {

        @Override
        public byte[] onAnycastMessageReceived(byte[] id, byte[] payload) {
            CallableResponse response = CallableResponse.parse(id, payload);
            DistributedTask<?> task = executionQueue.removePendingTask(UUIDUtils.parse(response.id));

            if (task != null) {
                executionQueue.removePendingTask(task.getId());

                if (response.hasError())
                    task.setException(response.throwable);
                else
                    task.set(response.outcome);
            }

            return null; // Server version does not handle synchronous replies
        }

        @Override
        public byte[] onBroadcastMessageReceived(byte[] id, byte[] payload) {
            // Not used right now
            return null; // Server version does not handle synchronous replies
        }
    }

}
