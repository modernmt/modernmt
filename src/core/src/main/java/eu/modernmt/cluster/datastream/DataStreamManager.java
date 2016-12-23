package eu.modernmt.cluster.datastream;

import eu.modernmt.engine.Engine;
import eu.modernmt.model.ImportJob;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.updating.UpdatesListener;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 06/09/16.
 */
public class DataStreamManager implements Closeable {

    public static final int DOMAIN_UPLOAD_STREAM_ID = 0;
    public static final String DOMAIN_UPLOAD_STREAM_TOPIC = "domain-upload-stream";
    private static final TopicPartition partition = new TopicPartition(DOMAIN_UPLOAD_STREAM_TOPIC, 0);

    private final Logger logger = LogManager.getLogger(DataStreamPollingThread.class);

    private final String uuid;
    private final DataStreamPollingThread pollingThread;
    private KafkaConsumer<Integer, StreamUpdate> consumer;
    private KafkaProducer<Integer, StreamUpdate> producer;

    private DataStreamListener listener;

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
        this.pollingThread = new DataStreamPollingThread(this, engine);
    }

    public void setDataStreamListener(DataStreamListener listener) {
        this.listener = listener;
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
        this.consumer.assign(Collections.singleton(partition));

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

    public ImportJob upload(int domainId, BilingualCorpus corpus) throws IOException, DataStreamException {
        if (this.producer == null)
            throw new IllegalStateException("connect() not called");

        this.pollingThread.ensureRunning();

        if (logger.isDebugEnabled())
            logger.debug("Uploading domain " + domainId);

        BilingualCorpus.BilingualLineReader reader = null;

        long importBegin, importEnd;
        int size = 0;

        try {
            reader = corpus.getContentReader();

            BilingualCorpus.StringPair pair = reader.read();
            if (pair == null)
                return null;

            importEnd = importBegin = sendUpdate(domainId, pair, true);
            size++;

            pair = reader.read();

            while (pair != null) {
                BilingualCorpus.StringPair current = pair;
                pair = reader.read();

                if (pair == null)
                    importEnd = sendUpdate(domainId, current, true);
                else
                    sendUpdate(domainId, current, false);

                size++;
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

        if (logger.isDebugEnabled())
            logger.debug("Domain " + domainId + " uploaded [" + importBegin + ", " + importEnd + "]: " + size + " pairs");

        ImportJob job = new ImportJob(domainId);
        job.setSize(size);
        job.setBegin(importBegin);
        job.setEnd(importEnd);

        return job;
    }

    private long sendUpdate(int domainId, BilingualCorpus.StringPair pair, boolean sync) throws DataStreamException {
        StreamUpdate update = new StreamUpdate(domainId, pair.source, pair.target);
        Future<RecordMetadata> future = producer.send(new ProducerRecord<>(DOMAIN_UPLOAD_STREAM_TOPIC, 0, update));

        long offset = -1L;

        if (sync) {
            try {
                offset = future.get().offset();
            } catch (InterruptedException e) {
                throw new DataStreamException("Could not complete upload for domain " + domainId, e);
            } catch (ExecutionException e) {
                throw new DataStreamException("Could not complete upload for domain " + domainId, e.getCause());
            }
        }

        return offset;
    }

    public void upload(int domainId, String sourceSentence, String targetSentence) throws DataStreamException {
        if (this.producer == null)
            throw new IllegalStateException("connect() not called");

        this.pollingThread.ensureRunning();

        StreamUpdate update = new StreamUpdate(domainId, sourceSentence, targetSentence);
        try {
            producer.send(new ProducerRecord<>(DOMAIN_UPLOAD_STREAM_TOPIC, 0, update))
                    .get();
        } catch (InterruptedException e) {
            throw new DataStreamException("Upload interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new DataStreamException("Unexpected exception while uploading", cause);
        }
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
            long offset = this.pollingThread.getCurrentOffset();

            if (offset >= position)
                break;

            Thread.sleep(500);
        }
    }

    // invoked by polling thread every time a new batch has been sent to its listeners
    void onUpdateReceived(long offset) {
        if (listener != null)
            listener.onUpdatesReveiced(offset);
    }

    private class ConnectionThread extends Thread {

        private long queueHead = 0L;

        public long getQueueHead() {
            return queueHead;
        }

        @Override
        public void run() {
            try {
                consumer.seekToEnd(Collections.singleton(partition));
                this.queueHead = consumer.position(partition);

                long offset = pollingThread.getCurrentOffset();

                logger.info("Topic " + partition.topic() + " seek to offset " + offset);
                consumer.seek(partition, offset);
            } catch (WakeupException e) {
                // Timeout occurred
            }
        }
    }
}
