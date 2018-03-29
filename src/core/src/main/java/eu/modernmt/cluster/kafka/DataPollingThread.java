package eu.modernmt.cluster.kafka;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.DataManagerException;
import eu.modernmt.engine.Engine;
import eu.modernmt.processing.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by davide on 06/09/16.
 */
class DataPollingThread extends Thread {

    private final Logger logger = LogManager.getLogger(KafkaDataManager.class);

    private final KafkaDataBatch batch;

    private DataManagerException exception;
    private KafkaConsumer<Integer, KafkaPacket> consumer;
    private boolean interrupted;
    private final ArrayList<DataListener> listeners = new ArrayList<>(10);
    private DataManager.Listener dataManagerListener = null;
    private KafkaDataManager manager;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public DataPollingThread(Engine engine, KafkaDataManager manager) {
        super("DataPollingThread");
        this.manager = manager;
        this.batch = new KafkaDataBatch(engine, manager);
    }

    public void ensureRunning() throws DataManagerException {
        DataManagerException e = exception;

        if (e != null)
            throw e;
    }

    public void setDataManagerListener(DataManager.Listener dataManagerListener) {
        this.dataManagerListener = dataManagerListener;
    }

    public void addListener(DataListener listener) {
        this.listeners.add(listener);
    }

    public void start(KafkaConsumer<Integer, KafkaPacket> consumer) {
        this.consumer = consumer;
        this.interrupted = false;

        super.start();
    }

    public void shutdown() {
        this.interrupted = true;
        this.consumer.wakeup();
    }

    public void shutdownNow() {
        this.shutdown();
        this.interrupt();
    }

    public boolean awaitTermination(TimeUnit unit, long timeout) throws InterruptedException {
        unit.timedJoin(this, timeout);
        return !this.isAlive();
    }

    public Map<Short, Long> getCurrentPositions() {
        KafkaChannel[] channels = this.manager.getChannels();

        HashMap<Short, Long> result = null;

        for (DataListener listener : listeners) {
            Map<Short, Long> latestPositions = listener.getLatestChannelPositions();

            logger.debug("DataListener[" + listener.getClass().getSimpleName() + "]: channel positions = " + latestPositions);

            if (latestPositions == null || latestPositions.isEmpty()) {
                result = null;
                break;
            }

            if (result == null) {
                result = new HashMap<>(latestPositions);
            } else {
                for (KafkaChannel channel : channels) {
                    short channelId = channel.getId();

                    Long v0 = result.get(channelId);
                    Long v1 = latestPositions.get(channelId);

                    long value = (v1 == null || v0 == null) ? -1L : Math.min(v0, v1);

                    result.put(channelId, value);
                }
            }
        }

        if (result == null)
            result = new HashMap<>();

        // Normalize result
        for (KafkaChannel channel : channels)
            result.putIfAbsent(channel.getId(), -1L);

        for (Map.Entry<Short, Long> entry : result.entrySet()) {
            long value = entry.getValue();
            entry.setValue(value < 0 ? 0 : value + 1);
        }

        return result;
    }

    @Override
    public void run() {
        while (!interrupted) {
            try {
                ConsumerRecords<Integer, KafkaPacket> records = consumer.poll(Long.MAX_VALUE);
                if (records.isEmpty())
                    continue;

                boolean process = false;
                boolean align = false;
                for (DataListener listener : listeners) {
                    if (process && align)
                        break;

                    process |= listener.needsProcessing();
                    align |= listener.needsAlignment();
                }

                batch.load(records, process, align);

                if (logger.isDebugEnabled())
                    logger.debug("Delivering batch of " + batch.size() + " updates");

                try {
                    deliverBatch(batch);
                } catch (Throwable e) {
                    logger.error("Failed to delivery updates", e);
                }

                if (dataManagerListener != null)
                    dataManagerListener.onDataBatchProcessed(batch.getChannelPositions());

                batch.clear();
            } catch (WakeupException e) {
                // Shutdown request
                break;
            } catch (RuntimeException e) {
                exception = new DataManagerException("Unexpected exception while data-stream polling", e);
                logger.error(exception.getMessage(), e);
                break;
            } catch (AlignerException | ProcessingException e) {
                exception = new DataManagerException("Failed to parse update batch", e);
                logger.error(exception.getMessage(), e);
                break;
            }
        }

        IOUtils.closeQuietly(consumer);
        executor.shutdownNow();
    }

    private void deliverBatch(KafkaDataBatch batch) throws Exception {
        if (listeners.isEmpty()) {
            logger.warn("Discarding " + batch.size() + " updates, listeners is empty");
            return;
        }

        int index = 0;
        Future[] results = new Future[listeners.size()];

        for (final DataListener listener : listeners)
            results[index++] = executor.submit(new DeliveryTask(batch, listener));

        for (Future<?> future : results) {
            try {
                future.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();

                if (cause instanceof Exception)
                    throw (Exception) cause;
                else
                    throw new Error("Unexpected exception", cause);
            }
        }

        if (logger.isDebugEnabled())
            logger.info("DataBatch delivered of size " + batch.size() + ", channels = " + batch.getChannelPositions());
    }

    private static final class DeliveryTask implements Callable<Void> {

        private final KafkaDataBatch batch;
        private final DataListener listener;

        public DeliveryTask(KafkaDataBatch batch, DataListener listener) {
            this.batch = batch;
            this.listener = listener;
        }

        @Override
        public Void call() throws Exception {
            listener.onDataReceived(batch);
            return null;
        }
    }

}
