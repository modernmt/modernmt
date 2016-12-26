package eu.modernmt.cluster.kafka;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.DataManagerException;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.engine.Engine;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.data.DataListener;
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

    public DataPollingThread(Engine engine) {
        super("DataPollingThread");
        this.batch = new DataBatch(engine);
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
        HashMap<Short, Long> result = new HashMap<>();

        for (DataListener listener : listeners) {
            Map<Short, Long> latestPositions = listener.getLatestChannelPositions();

            for (Map.Entry<Short, Long> entry : latestPositions.entrySet()) {
                Short channel = entry.getKey();
                Long position = entry.getValue();

                Long existentPosition = result.get(channel);
                if (existentPosition == null)
                    existentPosition = Long.MAX_VALUE;

                long value = (position == null) ? -1L : Math.min(existentPosition, position);
                result.put(channel, value);
            }
        }

        // Normalize result
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
    }

    private void deliverBatch(DataBatch batch) throws Exception {
        if (listeners.isEmpty()) {
            logger.warn("Discarding " + batch.size() + " updates, listeners is empty");
            return;
        }

        for (TranslationUnit unit : batch) {
            for (DataListener listener : listeners)
                listener.onDataReceived(unit);
        }
    }

}
