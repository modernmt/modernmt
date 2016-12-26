package eu.modernmt.cluster.kafka;

import eu.modernmt.data.*;
import eu.modernmt.engine.Engine;
import eu.modernmt.model.ImportJob;
import eu.modernmt.model.corpus.BilingualCorpus;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 06/09/16.
 */
public class KafkaDataManager implements DataManager {

    private static final Logger logger = LogManager.getLogger(KafkaDataManager.class);

    private static final KafkaChannel[] CHANNELS = new KafkaChannel[]{
            new KafkaChannel(DataManager.DOMAIN_UPLOAD_CHANNEL_ID, "domain-upload-stream"),
            new KafkaChannel(DataManager.CONTRIBUTIONS_CHANNEL_ID, "contributions-stream")
    };

    private static final ArrayList<TopicPartition> PARTITIONS = new ArrayList<>(CHANNELS.length);
    private static final HashMap<String, KafkaChannel> NAME_TO_CHANNEL = new HashMap<>(CHANNELS.length);

    static {
        for (KafkaChannel channel : CHANNELS) {
            PARTITIONS.add(channel.getTopicPartition());
            NAME_TO_CHANNEL.put(channel.getName(), channel);
        }
    }

    public static KafkaChannel getChannel(String name) {
        return NAME_TO_CHANNEL.get(name);
    }

    private final String uuid;
    private final DataPollingThread pollingThread;

    private KafkaConsumer<Integer, KafkaElement> consumer;
    private KafkaProducer<Integer, KafkaElement> producer;

    public KafkaDataManager(String uuid, Engine engine) {
        this.uuid = uuid;
        this.pollingThread = new DataPollingThread(engine);
    }

    private static Properties loadProperties(String filename, String host, int port) {
        InputStream stream = null;

        try {
            Properties properties = new Properties();
            stream = KafkaDataManager.class.getClassLoader().getResourceAsStream(filename);
            properties.load(stream);

            properties.put("bootstrap.servers", host + ":" + port);
            properties.put("key.serializer", IntegerSerializer.class.getName());
            properties.put("value.serializer", KafkaElementSerializer.class.getName());
            properties.put("key.deserializer", IntegerDeserializer.class.getName());
            properties.put("value.deserializer", KafkaElementDeserializer.class.getName());

            return properties;
        } catch (IOException e) {
            throw new Error("Unexpected exception", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @Override
    public Map<Short, Long> connect(long timeout, TimeUnit unit) throws HostUnreachableException {
        return connect("localhost", 9092, timeout, unit);
    }

    @Override
    public Map<Short, Long> connect(String host, int port, long timeout, TimeUnit unit) throws HostUnreachableException {
        Properties producerProperties = loadProperties("kafka-producer.properties", host, port);
        this.producer = new KafkaProducer<>(producerProperties);

        Properties consumerProperties = loadProperties("kafka-consumer.properties", host, port);
        consumerProperties.put("group.id", uuid);

        this.consumer = new KafkaConsumer<>(consumerProperties);
        this.consumer.assign(PARTITIONS);

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

        return connectThread.getLatestPositions();
    }

    @Override
    public void setDataManagerListener(Listener listener) {
        pollingThread.setDataManagerListerner(listener);
    }

    @Override
    public void addDataListener(DataListener listener) {
        pollingThread.addListener(listener);
    }

    @Override
    public ImportJob upload(int domainId, BilingualCorpus corpus, short channel) throws DataManagerException {
        return upload(domainId, corpus, getDataChannel(channel));
    }

    @Override
    public ImportJob upload(int domainId, BilingualCorpus corpus, DataChannel channel) throws DataManagerException {
        if (this.producer == null)
            throw new IllegalStateException("connect() not called");

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

            importEnd = importBegin = sendUpdate(domainId, pair.source, pair.target, true, channel);
            size++;

            pair = reader.read();

            while (pair != null) {
                BilingualCorpus.StringPair current = pair;
                pair = reader.read();

                if (pair == null)
                    importEnd = sendUpdate(domainId, current.source, current.target, true, channel);
                else
                    sendUpdate(domainId, current.source, current.target, false, channel);

                size++;
            }
        } catch (IOException e) {
            throw new DataManagerException("Failed to read corpus for domain " + domainId, e);
        } finally {
            IOUtils.closeQuietly(reader);
        }

        if (logger.isDebugEnabled())
            logger.debug("Domain " + domainId + " uploaded [" + importBegin + ", " + importEnd + "]: " + size + " pairs");

        ImportJob job = new ImportJob(domainId);
        job.setSize(size);
        job.setDataChannel(channel.getId());
        job.setBegin(importBegin);
        job.setEnd(importEnd);

        return job;
    }

    @Override
    public void upload(int domainId, String sourceSentence, String targetSentence, short channel) throws DataManagerException {
        upload(domainId, sourceSentence, targetSentence, getDataChannel(channel));
    }

    @Override
    public void upload(int domainId, String sourceSentence, String targetSentence, DataChannel channel) throws DataManagerException {
        if (this.producer == null)
            throw new IllegalStateException("connect() not called");

        sendUpdate(domainId, sourceSentence, targetSentence, true, channel);
    }

    private long sendUpdate(int domainId, String source, String target, boolean sync, DataChannel channel) throws DataManagerException {
        pollingThread.ensureRunning();

        KafkaElement update = new KafkaElement(domainId, source, target);
        Future<RecordMetadata> future = producer.send(new ProducerRecord<>(channel.getName(), 0, update));

        long offset = -1L;

        if (sync) {
            try {
                offset = future.get().offset();
            } catch (InterruptedException e) {
                throw new DataManagerException("Interrupted upload for domain " + domainId, e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                else
                    throw new DataManagerException("Unexpected exception while uploading", cause);
            }
        }

        return offset;
    }

    @Override
    public KafkaChannel getDataChannel(short id) {
        return CHANNELS[id];
    }

    @Override
    public void waitChannelPosition(short channel, long position) throws InterruptedException {
        HashMap<Short, Long> map = new HashMap<>(1);
        map.put(channel, position);

        waitChannelPositions(map);
    }

    @Override
    public void waitChannelPositions(Map<Short, Long> positions) throws InterruptedException {
        while (true) {
            Map<Short, Long> current = this.pollingThread.getCurrentPositions();
            boolean wait = false;

            for (Map.Entry<Short, Long> entry : positions.entrySet()) {
                Short channel = entry.getKey();
                Long targetPosition = entry.getValue();

                if (targetPosition == 0L)
                    continue;

                Long position = current.get(channel);

                if (position == null || position < targetPosition) {
                    wait = true;
                    break;
                }
            }

            if (!wait)
                break;

            Thread.sleep(500);
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

    private class ConnectionThread extends Thread {

        private HashMap<Short, Long> positions = new HashMap<>(CHANNELS.length);

        private HashMap<Short, Long> getLatestPositions() {
            return positions;
        }

        @Override
        public void run() {
            try {
                consumer.seekToEnd(PARTITIONS);

                for (KafkaChannel channel : CHANNELS)
                    positions.put(channel.getId(), consumer.position(channel.getTopicPartition()));

                for (Map.Entry<Short, Long> entry : pollingThread.getCurrentPositions().entrySet()) {
                    KafkaChannel channel = getDataChannel(entry.getKey());
                    long position = entry.getValue();

                    logger.info("Channel '" + channel.getName() + "' seek to position " + position);
                    consumer.seek(channel.getTopicPartition(), position);
                }
            } catch (WakeupException e) {
                // Timeout occurred
            }
        }
    }
}
