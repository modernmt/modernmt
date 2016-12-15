package eu.modernmt.cluster.datastream;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.engine.Engine;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.updating.Update;
import eu.modernmt.updating.UpdatesListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 06/09/16.
 */
class DataStreamPollingThread extends Thread {

    private final Logger logger = LogManager.getLogger(DataStreamPollingThread.class);

    private final DataStreamManager manager;
    private final UpdateBatch batch;

    private DataStreamException exception;
    private KafkaConsumer<Integer, StreamUpdate> consumer;
    private boolean interrupted;
    private final ArrayList<UpdatesListener> listeners = new ArrayList<>(10);

    public DataStreamPollingThread(DataStreamManager manager, Engine engine) {
        super("DataStreamPollingThread");
        this.batch = new UpdateBatch(engine);
        this.manager = manager;
    }

    public void ensureRunning() throws DataStreamException {
        DataStreamException e = exception;

        if (e != null)
            throw e;
    }

    public void addListener(UpdatesListener listener) {
        this.listeners.add(listener);
    }

    public void start(KafkaConsumer<Integer, StreamUpdate> consumer) {
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

    public long getCurrentOffset() {
        long offset = Long.MAX_VALUE;

        for (UpdatesListener listener : listeners) {
            Map<Integer, Long> map = listener.getLatestSequentialNumbers();

            Long seqId = map.get(DataStreamManager.DOMAIN_UPLOAD_STREAM_ID);
            offset = (seqId == null) ? -1L : Math.min(offset, seqId);
        }

        return offset < 0 ? 0 : offset + 1;
    }

    @Override
    public void run() {
        while (!interrupted) {
            try {
                ConsumerRecords<Integer, StreamUpdate> records = consumer.poll(Long.MAX_VALUE);
                if (records.isEmpty())
                    continue;

                batch.load(records);
                long offset = batch.getCurrentOffset();

                if (logger.isDebugEnabled())
                    logger.debug("Delivering batch #" + offset + " of " + batch.size() + " updates");

                try {
                    deliveryUpdate(batch);
                } catch (Throwable e) {
                    logger.error("Failed to delivery updates", e);
                }

                manager.onUpdateReceived(offset);
                batch.clear();
            } catch (WakeupException e) {
                // Shutdown request
                break;
            } catch (RuntimeException e) {
                exception = new DataStreamException("Unexpected exception while data-stream polling", e);
                break;
            } catch (AlignerException | ProcessingException e) {
                exception = new DataStreamException("Failed to parse update batch", e);
                break;
            }
        }
    }

    private void deliveryUpdate(UpdateBatch batch) throws Exception {
        if (listeners.isEmpty()) {
            logger.warn("Discarding " + batch.size() + " updates, listeners is empty");
            return;
        }

        for (Update update : batch) {
            for (UpdatesListener listener : listeners)
                listener.updateReceived(update);
        }
    }

}
