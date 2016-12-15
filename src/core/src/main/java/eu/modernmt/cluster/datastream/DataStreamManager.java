package eu.modernmt.cluster.datastream;

import eu.modernmt.engine.Engine;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.updating.UpdatesListener;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 06/09/16.
 */
public class DataStreamManager implements Closeable {

    private static final int DOMAIN_UPLOAD_STREAM = 0;

    static final String[] TOPICS = new String[]{
            "domain-upload-stream",
    };

    private final Logger logger = LogManager.getLogger(DataStreamPollingThread.class);

    private static final TopicPartition[] partitions = getPartitions();

    private static TopicPartition[] getPartitions() {
        TopicPartition[] partitions = new TopicPartition[DataStreamManager.TOPICS.length];
        for (int i = 0; i < partitions.length; i++)
            partitions[i] = new TopicPartition(DataStreamManager.TOPICS[i], 0);
        return partitions;
    }

    private final String uuid;
    private final DataStreamPollingThread pollingThread;
    private KafkaConsumer<Integer, StreamUpdate> consumer;
    private KafkaProducer<Integer, StreamUpdate> producer;

    private static Properties loadProperties(String filename, String host, int port) {
        InputStream stream = null;

        try {
            Properties properties = new Properties();
            stream = DataStreamManager.class.getClassLoader().getResourceAsStream(filename);
            properties.load(stream);

            properties.put("bootstrap.servers", host + ":" + port);
            properties.put("key.serializer", IntegerSerializer.class.getName());
            properties.put("value.serializer", StreamUpdateSerializer.class.getName());
            properties.put("key.deserializer", IntegerDeserializer.class.getName());
            properties.put("value.deserializer", StreamUpdateDeserializer.class.getName());

            return properties;
        } catch (IOException e) {
            throw new Error("Unexpected exception", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public DataStreamManager(String uuid, Engine engine) {
        this.uuid = uuid;
        this.pollingThread = new DataStreamPollingThread(engine);
    }

    public void addListener(UpdatesListener listener) {
        this.pollingThread.addListener(listener);
    }

    @Deprecated
    public long connect(long timeout, TimeUnit unit) throws HostUnreachableException {
        return connect("localhost", 9092, timeout, unit);
    }

    public long connect(String host, int port, long timeout, TimeUnit unit) throws HostUnreachableException {
        Properties producerProperties = loadProperties("kafka-producer.properties", host, port);
        this.producer = new KafkaProducer<>(producerProperties);

        Properties consumerProperties = loadProperties("kafka-consumer.properties", host, port);
        consumerProperties.put("group.id", uuid);

        this.consumer = new KafkaConsumer<>(consumerProperties);
        this.consumer.assign(Arrays.asList(partitions));

        ConnectionThread connectThread = new ConnectionThread();
        connectThread.start();

        try {
            unit.timedJoin(connectThread, timeout);
        } catch (InterruptedException e) {
            // Ignore it
        }

        if (connectThread.isAlive())
            throw new HostUnreachableException(host + ':' + port);

        this.pollingThread.start(this.consumer);

        return connectThread.getQueueHead();
    }

    public int upload(int domainId, BilingualCorpus corpus) throws IOException, DataStreamException {
        if (this.producer == null)
            throw new IllegalStateException("connect() not called");

        this.pollingThread.ensureRunning();

        if (logger.isDebugEnabled())
            logger.debug("Uploading domain " + domainId);

        int count = 0;
        BilingualCorpus.BilingualLineReader reader = null;

        try {
            reader = corpus.getContentReader();

            BilingualCorpus.StringPair pair = reader.read();
            if (pair == null)
                return 0;

            StreamUpdate update = new StreamUpdate(domainId, pair.source, pair.target, false, true);

            while ((pair = reader.read()) != null) {
                producer.send(new ProducerRecord<>(TOPICS[DOMAIN_UPLOAD_STREAM], 0, update));
                count++;

                update = new StreamUpdate(domainId, pair.source, pair.target);
            }

            update.setLast(true);
            producer.send(new ProducerRecord<>(TOPICS[DOMAIN_UPLOAD_STREAM], 0, update));
            count++;
        } finally {
            IOUtils.closeQuietly(reader);
        }

        if (logger.isDebugEnabled())
            logger.debug("Domain " + domainId + " uploaded: " + count + " lines sent.");

        return count;
    }

    public void upload(int domainId, String sourceSentence, String targetSentence) throws DataStreamException {
        if (this.producer == null)
            throw new IllegalStateException("connect() not called");

        this.pollingThread.ensureRunning();

        StreamUpdate update = new StreamUpdate(domainId, sourceSentence, targetSentence);
        producer.send(new ProducerRecord<>(TOPICS[DOMAIN_UPLOAD_STREAM], 0, update));
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(consumer);
        IOUtils.closeQuietly(producer);

        pollingThread.shutdown();
        try {
            if (!pollingThread.awaitTermination(TimeUnit.SECONDS, 2))
                pollingThread.shutdownNow();
        } catch (InterruptedException e) {
            pollingThread.shutdownNow();
        }
    }

    public void waitQueuePosition(long position) throws InterruptedException {
        while (true) {
            long offset = this.pollingThread.getCurrentOffsets(partitions)[0];

            if (offset >= position)
                break;

            Thread.sleep(500);
        }
    }

    private class ConnectionThread extends Thread {

        private long queueHead = 0L;

        public long getQueueHead() {
            return queueHead;
        }

        @Override
        public void run() {
            try {
                consumer.seekToEnd(Arrays.asList(partitions));
                this.queueHead = consumer.position(partitions[0]);

                long[] offsets = pollingThread.getCurrentOffsets(partitions);

                for (int i = 0; i < offsets.length; i++) {
                    logger.info("Topic " + partitions[i].topic() + " seek to offset " + offsets[i]);
                    consumer.seek(partitions[i], offsets[i]);
                }
            } catch (WakeupException e) {
                // Timeout occurred
            }
        }
    }
}
