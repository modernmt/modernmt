package eu.modernmt.cluster.datastream;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.engine.Engine;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.updating.Update;
import eu.modernmt.updating.UpdatesListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 06/09/16.
 */
class DataStreamPollingThread extends Thread {

    private final Logger logger = LogManager.getLogger(DataStreamPollingThread.class);

    private final UpdateBatch batch;
    private DataStreamException exception;
    private KafkaConsumer<Integer, StreamUpdate> consumer;
    private boolean interrupted;
    private final ArrayList<UpdatesListener> listeners = new ArrayList<>(10);

    public DataStreamPollingThread(Engine engine) {
        super("DataStreamPollingThread");
        batch = new UpdateBatch(engine);
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

    public long[] getCurrentOffsets(TopicPartition[] partitions) {
        long[] offsets = new long[partitions.length];
        Arrays.fill(offsets, Long.MAX_VALUE);

        for (UpdatesListener listener : listeners) {
            Map<Integer, Long> map = listener.getLatestSequentialNumbers();

            for (int i = 0; i < offsets.length; i++) {
                Long seqId = map.get(i);
                offsets[i] = (seqId == null) ? -1L : Math.min(offsets[i], seqId);
            }
        }

        for (int i = 0; i < offsets.length; i++)
            offsets[i] = offsets[i] < 0 ? 0 : offsets[i] + 1;

        return offsets;
    }

    @Override
    public void run() {
        while (!interrupted) {
            try {
                ConsumerRecords<Integer, StreamUpdate> records = consumer.poll(Long.MAX_VALUE);
                if (records.isEmpty())
                    continue;

                batch.load(records);

                if (logger.isDebugEnabled())
                    logger.debug("Delivering batch of " + batch.size() + " updates");

                try {
                    deliveryUpdate(batch);
                } catch (Throwable e) {
                    logger.error("Failed to delivery updates", e);
                }
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
