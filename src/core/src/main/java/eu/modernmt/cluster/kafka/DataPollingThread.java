package eu.modernmt.cluster.kafka;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.data.*;
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
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 06/09/16.
 */
class DataPollingThread extends Thread {

    private final Logger logger = LogManager.getLogger(KafkaDataManager.class);

    private final DataBatch batch;

    private DataManagerException exception;
    private KafkaConsumer<Integer, KafkaElement> consumer;
    private boolean interrupted;
    private final ArrayList<DataListener> listeners = new ArrayList<>(10);
    private DataManager.Listener dataManagerListener = null;
    private KafkaDataManager manager;

    public DataPollingThread(Engine engine, KafkaDataManager manager) {
        super("DataPollingThread");
        this.manager = manager;
        this.batch = new DataBatch(engine, manager);
    }

    public void ensureRunning() throws DataManagerException {
        DataManagerException e = exception;

        if (e != null)
            throw e;
    }

    public void setDataManagerListener(DataManager.Listener dataManagerListerner) {
        this.dataManagerListener = dataManagerListerner;
    }

    public void addListener(DataListener listener) {
        this.listeners.add(listener);
    }

    public void start(KafkaConsumer<Integer, KafkaElement> consumer) {
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
            Map<Short, Long> latestPositions = new HashMap<>(listener.getLatestChannelPositions());

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
                ConsumerRecords<Integer, KafkaElement> records = consumer.poll(Long.MAX_VALUE);
                if (records.isEmpty())
                    continue;

                batch.load(records);

                if (logger.isDebugEnabled())
                    logger.debug("Delivering batch of " + batch.size() + " updates");

                try {
                    deliverBatch(batch);
                } catch (Throwable e) {
                    logger.error("Failed to delivery updates", e);
                }

                if (dataManagerListener != null)
                    dataManagerListener.onDataBatchProcessed(batch.getBatchOffset());

                batch.clear();
            } catch (WakeupException e) {
                // Shutdown request
                break;
            } catch (RuntimeException e) {
                exception = new DataManagerException("Unexpected exception while data-stream polling", e);
                break;
            } catch (AlignerException | ProcessingException e) {
                exception = new DataManagerException("Failed to parse update batch", e);
                break;
            }
        }

        IOUtils.closeQuietly(consumer);
    }

    private void deliverBatch(DataBatch batch) throws Exception {
        if (listeners.isEmpty()) {
            logger.warn("Discarding " + batch.size() + " updates, listeners is empty");
            return;
        }

        for (TranslationUnit unit : batch.getTranslationUnits()) {
            for (DataListener listener : listeners)
                listener.onDataReceived(unit);
        }

        for (Deletion deletion : batch.getDeletions()) {
            for (DataListener listener : listeners)
                listener.onDelete(deletion);
        }
    }
}
