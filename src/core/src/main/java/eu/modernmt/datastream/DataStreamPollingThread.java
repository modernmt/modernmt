package eu.modernmt.datastream;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.engine.Engine;
import eu.modernmt.processing.ProcessingException;
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
    private final ArrayList<DataStreamListener> listeners = new ArrayList<>(10);

    public DataStreamPollingThread(Engine engine) {
        super("DataStreamPollingThread");
        batch = new UpdateBatch(engine);
    }

    public void ensureRunning() throws DataStreamException {
        DataStreamException e = exception;

        if (e != null)
            throw e;
    }

    public void addListener(DataStreamListener listener) {
        this.listeners.add(listener);
    }

    private static TopicPartition[] getPartitions() {
        TopicPartition[] partitions = new TopicPartition[DataStreamManager.TOPICS.length];
        for (int i = 0; i < partitions.length; i++)
            partitions[i] = new TopicPartition(DataStreamManager.TOPICS[i], 0);
        return partitions;
    }

    public void start(KafkaConsumer<Integer, StreamUpdate> consumer) {
        this.consumer = consumer;
        this.interrupted = false;

        TopicPartition[] partitions = getPartitions();

        this.consumer.assign(Arrays.asList(partitions));

        long[] offsets = new long[partitions.length];
        for (DataStreamListener listener : listeners) {
            for (Map.Entry<Integer, Long> entry : listener.getLatestSequentialNumbers().entrySet()) {
                int id = entry.getKey();
                long num = entry.getValue();

                if (id < offsets.length && offsets[id] > num)
                    offsets[id] = num;
            }
        }

        for (int i = 0; i < offsets.length; i++)
            consumer.seek(partitions[i], offsets[i]);

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
            for (DataStreamListener listener : listeners)
                listener.updateReceived(update);
        }
    }

}
