package eu.modernmt.cluster.kafka;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.config.BinaryLogConfig;
import eu.modernmt.data.*;
import eu.modernmt.engine.Engine;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.model.ImportJob;
import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.processing.Preprocessor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
 * Updated by andrearossi on 29/03/17
 * <p>
 * This class manages the Apache Kafka environment:
 * it handles the Kafka channels, topics and partitions,
 * it creates the kafka producer and consumer for sending and reading messages,
 * and starts separate threads to connect to the Apache Kafka server
 * to perform polling in order to find the proper positions on the topics
 */
public class KafkaBinaryLog implements BinaryLog {

    private static final Logger logger = LogManager.getLogger(KafkaBinaryLog.class);

    private final String[] hosts;
    private final int port;
    private final String name;  // the base name of the kafka topics

    private final String uuid;
    private final LogDataPollingThread pollingThread;

    private KafkaProducer<Integer, KafkaPacket> producer;

    private KafkaChannel[] channels;
    private ArrayList<TopicPartition> partitions;
    private HashMap<String, KafkaChannel> name2channel;

    private static Aligner getAligner(Engine engine) {
        try {
            return engine.getAligner();
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    public KafkaBinaryLog(Engine engine, String uuid, BinaryLogConfig config) {
        this(engine.getLanguageIndex(), engine.getPreprocessor(), getAligner(engine), uuid, config);
    }

    public KafkaBinaryLog(LanguageIndex languages, Preprocessor preprocessor, Aligner aligner, String uuid, BinaryLogConfig config) {
        this.uuid = uuid;

        this.hosts = config.getHosts();
        this.port = config.getPort();
        this.name = config.getName();

        this.pollingThread = new LogDataPollingThread(languages, preprocessor, aligner, this);

        // initialize the two required kafkaChannels with proper names
        // and put them in an array "channels"
        this.channels = new KafkaChannel[2];

        String[] topicNames = getDefaultTopicNames(this.name);

        this.channels[0] = new KafkaChannel(BinaryLog.MEMORY_UPLOAD_CHANNEL_ID,
                topicNames[BinaryLog.MEMORY_UPLOAD_CHANNEL_ID]);
        this.channels[1] = new KafkaChannel(BinaryLog.CONTRIBUTIONS_CHANNEL_ID,
                topicNames[BinaryLog.CONTRIBUTIONS_CHANNEL_ID]);

        /*initialize and populate the partitions list and the name-to-channel map*/
        this.partitions = new ArrayList<>(channels.length);
        this.name2channel = new HashMap<>(channels.length);
        for (KafkaChannel channel : this.channels) {
            this.partitions.add(channel.getTopicPartition());
            this.name2channel.put(channel.getName(), channel);
        }
    }

    /**
     * This method calculates default acceptable names for all kafka topics
     * and puts them in an ordered String array.
     * The array order is:
     * 0: memories topic name
     * 1: contributions topic name
     * (using an array ensures high access speed)
     *
     * @return the array with all topic names (Strings)
     */
    public static String[] getDefaultTopicNames(String prefix) {

        //max topic name length supported by Apache Kafka
        int maxLength = 249;

        //default names for the topics
        String memoriesTopicName = "memory-upload-stream";
        String contributionsTopicName = "contributions-stream";

        //if prefix is not null, normalize it and update the topic names
        if (prefix != null) {
            String normalizedPrefix = prefix.replaceAll("[^A-Za-z0-9_\\.\\-]", "");

            int length = Math.max(
                    normalizedPrefix.length() + memoriesTopicName.length() + 1,
                    normalizedPrefix.length() + contributionsTopicName.length()) + 1;

            /*if even only one of the two names are too long,
             * cut away a part of the engine name from both of them*/
            if (length > maxLength)
                normalizedPrefix = normalizedPrefix.substring(0, length - maxLength - 1).replaceAll("/[^A-Za-z0-9\\._\\-]/", "");

            memoriesTopicName = normalizedPrefix + "-" + memoriesTopicName;
            contributionsTopicName = normalizedPrefix + "-" + contributionsTopicName;
        }

        /*create, populate and return the topics map*/
        String[] topicNames = new String[2];
        topicNames[BinaryLog.MEMORY_UPLOAD_CHANNEL_ID] = memoriesTopicName;
        topicNames[BinaryLog.CONTRIBUTIONS_CHANNEL_ID] = contributionsTopicName;
        return topicNames;
    }

    public static Properties loadProperties(String filename, String[] hosts, int port) {
        InputStream stream = null;

        try {
            Properties properties = new Properties();
            stream = KafkaBinaryLog.class.getClassLoader().getResourceAsStream(filename);
            properties.load(stream);

            String[] servers = new String[hosts.length];
            for (int i = 0; i < servers.length; i++)
                servers[i] = hosts[i] + ':' + port;

            properties.put("bootstrap.servers", StringUtils.join(servers, ','));
            properties.put("key.serializer", IntegerSerializer.class.getName());
            properties.put("value.serializer", KafkaPacketSerializer.class.getName());
            properties.put("key.deserializer", IntegerDeserializer.class.getName());
            properties.put("value.deserializer", KafkaPacketDeserializer.class.getName());

            return properties;
        } catch (IOException e) {
            throw new Error("Unexpected exception", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    /**
     * This method makes this KafkaBinaryLog connect to the Kafka server specified in its initial BinaryLogConfig.
     * To connect, a default timeout of 60 seconds is employed.
     * If the connection succeeded, this method returns a map that associates to the (short) ID of each kafka topic
     * the corresponding (long) last written position.
     *
     * @return a map containing, for each Kafka topic, its latest written positions if a consumer was to be started; null if only the producer was to be started.
     * @throws HostUnreachableException if could not connect to the Kafka server.
     */
    @Override
    public Map<Short, Long> connect() throws HostUnreachableException {
        return this.connect(60, TimeUnit.SECONDS, true, true);
    }


    /**
     * This method makes this KafkaBinaryLog connect to the Kafka server specified in its initial BinaryLogConfig.
     * It launches both a kafka consumer and a kafka producer to interact with the Kafka server.
     * <p>
     * The connection process will use the passed timeout and timeunit values.
     * <p>
     * If the connection succeeds, this method returns a map that associates to the (short) ID of each kafka topic
     * the corresponding (long) last written position.
     *
     * @param timeout timeout to use while connecting to the Kafka server
     * @param unit    time unit for the timeout
     * @return a map containing, for each Kafka topic, its latest written positions if a consumer was to be started; null if only the producer was to be started.
     * @throws HostUnreachableException if could not connect to the Kafka server
     */
    @Override
    public Map<Short, Long> connect(long timeout, TimeUnit unit) throws HostUnreachableException {
        return this.connect(timeout, unit, true, true);
    }

    /**
     * This method makes this KafkaBinaryLog connect to the Kafka server specified in its initial BinaryLogConfig.
     * <p>
     * A kafka consumer and/or a kafka producer will be launched depending on the passed "enable" params.
     * If both the "enable" params are false, this method will do nothing.
     * <p>
     * The connection process will use the passed timeout and timeunit values.
     * <p>
     * If the connection succeeds, this method returns a map that associates to the (short) ID of each kafka topic
     * the corresponding (long) last written position.
     *
     * @param timeout        timeout to use while connecting to the Kafka server
     * @param unit           time unit for the timeout
     * @param enableConsumer boolean value that specifies whether the kafka consumer should be started or not
     * @param enableProducer boolean value that specifies whether the kafka producer should be started or not
     * @return a map containing, for each Kafka topic, its latest written positions if a consumer was to be started; null if only the producer was to be started.
     * @throws HostUnreachableException if could not connect to the Kafka server
     */
    @Override
    public Map<Short, Long> connect(long timeout, TimeUnit unit, boolean enableConsumer, boolean enableProducer) throws HostUnreachableException {

        // Create Kafka producer
        if (enableProducer) {
            Properties producerProperties = loadProperties("kafka-producer.properties", hosts, port);
            this.producer = new KafkaProducer<>(producerProperties);    //write in the given partitions
        }

        // Create Kafka consumer and connect to the Kafka remote server to get the latest positions for each channel
        if (enableConsumer) {
            // load consumer properties and build kafka consumer for reading messages from the server from the given partitions
            Properties consumerProperties = loadProperties("kafka-consumer.properties", hosts, port);
            consumerProperties.put("group.id", uuid);
            KafkaConsumer<Integer, KafkaPacket> consumer = new KafkaConsumer<>(consumerProperties);
            consumer.assign(partitions);

            //use a separate thread to connect to the Kafka server
            ConnectionThread connectThread = new ConnectionThread(consumer);
            connectThread.start();
            try {
                unit.timedJoin(connectThread, timeout);
            } catch (InterruptedException e) {
                // ignore it
            }

            if (connectThread.isAlive())    // if the thread is still alive could not connect to the Kafka server
                throw new HostUnreachableException(hosts, port);

            this.pollingThread.start(consumer);

            return connectThread.getLatestPositions();
        }
        return null;
    }

    @Override
    public void setBinaryLogListener(Listener listener) {
        pollingThread.setBinaryLogListener(listener);
    }

    @Override
    public void addLogDataListener(LogDataListener listener) {
        pollingThread.addListener(listener);
    }

    @Override
    public ImportJob upload(Memory memory, MultilingualCorpus corpus, short channel) throws BinaryLogException {
        return upload(memory, corpus, getLogChannel(channel));
    }

    @Override
    public ImportJob upload(Memory memory, MultilingualCorpus corpus, LogChannel channel) throws BinaryLogException {
        if (this.producer == null)
            throw new IllegalStateException("connect() not called");

        if (logger.isDebugEnabled())
            logger.debug("Uploading memory " + memory);

        MultilingualCorpus.MultilingualLineReader reader = null;

        long importBegin, importEnd;
        int size = 0;

        try {
            reader = corpus.getContentReader();

            MultilingualCorpus.StringPair pair = reader.read();
            if (pair == null)
                return null;

            importEnd = importBegin = sendElement(KafkaPacket.createAddition(pair.language, memory.getOwner(), memory.getId(), pair.source, pair.target, pair.timestamp), true, channel);
            size++;

            pair = reader.read();

            while (pair != null) {
                MultilingualCorpus.StringPair current = pair;
                pair = reader.read();

                if (pair == null)
                    importEnd = sendElement(KafkaPacket.createAddition(current.language, memory.getOwner(), memory.getId(), current.source, current.target, current.timestamp), true, channel);
                else
                    sendElement(KafkaPacket.createAddition(current.language, memory.getOwner(), memory.getId(), current.source, current.target, current.timestamp), false, channel);

                size++;
            }
        } catch (IOException e) {
            throw new BinaryLogException("Failed to read corpus for memory " + memory, e);
        } finally {
            IOUtils.closeQuietly(reader);
        }

        if (logger.isDebugEnabled())
            logger.debug("Memory " + memory + " uploaded [" + importBegin + ", " + importEnd + "]: " + size + " pairs");

        ImportJob job = new ImportJob();
        job.setMemory(memory.getId());
        job.setSize(size);
        job.setDataChannel(channel.getId());
        job.setBegin(importBegin);
        job.setEnd(importEnd);

        return job;
    }

    @Override
    public ImportJob upload(LanguageDirection direction, Memory memory, String sentence, String translation, Date timestamp, short channel) throws BinaryLogException {
        return upload(direction, memory, sentence, translation, timestamp, getLogChannel(channel));
    }

    @Override
    public ImportJob upload(LanguageDirection direction, Memory memory, String sentence, String translation, Date timestamp, LogChannel channel) throws BinaryLogException {
        if (this.producer == null)
            throw new IllegalStateException("connect() not called");
        long offset = sendElement(KafkaPacket.createAddition(direction, memory.getOwner(), memory.getId(), sentence, translation, timestamp), true, channel);
        return ImportJob.createEphemeralJob(memory.getId(), offset, channel.getId());
    }

    @Override
    public ImportJob replace(LanguageDirection direction, Memory memory, String sentence, String translation, String previousSentence, String previousTranslation, Date timestamp, short channel) throws BinaryLogException {
        return replace(direction, memory, sentence, translation, previousSentence, previousTranslation, timestamp, getLogChannel(channel));
    }

    @Override
    public ImportJob replace(LanguageDirection direction, Memory memory, String sentence, String translation, String previousSentence, String previousTranslation, Date timestamp, LogChannel channel) throws BinaryLogException {
        if (this.producer == null)
            throw new IllegalStateException("connect() not called");

        long offset = sendElement(KafkaPacket.createOverwrite(direction, memory.getOwner(), memory.getId(), sentence, translation, previousSentence, previousTranslation, timestamp), true, channel);
        return ImportJob.createEphemeralJob(memory.getId(), offset, channel.getId());
    }

    @Override
    public void delete(long memory) throws BinaryLogException {
        if (this.producer == null)
            throw new IllegalStateException("connect() not called");

        LogChannel channel = getLogChannel(BinaryLog.MEMORY_UPLOAD_CHANNEL_ID);
        sendElement(KafkaPacket.createDeletion(memory), true, channel);
    }

    private long sendElement(KafkaPacket packet, boolean sync, LogChannel channel) throws BinaryLogException {
        pollingThread.ensureRunning();

        Future<RecordMetadata> future = producer.send(new ProducerRecord<>(channel.getName(), 0, packet));

        long offset = -1L;

        if (sync) {
            try {
                offset = future.get().offset();
            } catch (InterruptedException e) {
                throw new BinaryLogException("Interrupted upload for packet " + packet, e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException)
                    throw (RuntimeException) cause;
                else
                    throw new BinaryLogException("Unexpected exception while uploading", cause);
            }
        }

        return offset;
    }

    @Override
    public KafkaChannel getLogChannel(short id) {
        return this.channels[id];
    }

    @Override
    public Map<Short, Long> getChannelsPositions() {
        return pollingThread.getCurrentPositions();
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
        pollingThread.shutdown();
        try {
            if (!pollingThread.awaitTermination(TimeUnit.SECONDS, 2))
                pollingThread.shutdownNow();
        } catch (InterruptedException e) {
            pollingThread.shutdownNow();
        }

        IOUtils.closeQuietly(producer);
    }

    public KafkaChannel getChannel(String name) {
        return name2channel.get(name);
    }


    public KafkaChannel[] getChannels() {
        return channels;
    }


    private class ConnectionThread extends Thread {

        private final KafkaConsumer<Integer, KafkaPacket> consumer;
        private HashMap<Short, Long> positions = new HashMap<>(channels.length);

        private ConnectionThread(KafkaConsumer<Integer, KafkaPacket> consumer) {
            this.consumer = consumer;
        }

        private HashMap<Short, Long> getLatestPositions() {
            return positions;
        }

        @Override
        public void run() {
            try {
                consumer.seekToEnd(partitions);

                for (KafkaChannel channel : channels) {
                    positions.put(channel.getId(), consumer.position(channel.getTopicPartition()));
                }

                for (Map.Entry<Short, Long> entry : pollingThread.getCurrentPositions().entrySet()) {

                    KafkaChannel channel = getLogChannel(entry.getKey());
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