package eu.modernmt.datastream;

import eu.modernmt.engine.Engine;
import eu.modernmt.model.corpus.BilingualCorpus;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 06/09/16.
 */
public class DataStreamManager implements Closeable {

    private static final int DOMAIN_UPLOAD_STREAM = 0;
    private static final int UPDATE_STREAM = 1;

    static final String[] TOPICS = new String[]{
            "domain-upload-stream",
            "update-stream"
    };

    private final Logger logger = LogManager.getLogger(DataStreamPollingThread.class);


    private final String uuid;
    private final DataStreamPollingThread pollingThread;
    private KafkaConsumer<Integer, StreamUpdate> consumer;
    private KafkaProducer<Integer, StreamUpdate> producer;

    private static Properties loadProperties(String filename, String host, int port) throws IOException {
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
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public DataStreamManager(String uuid, Engine engine) {
        this.uuid = uuid;
        this.pollingThread = new DataStreamPollingThread(engine);
    }

    public void addListener(DataStreamListener listener) {
        this.pollingThread.addListener(listener);
    }

    @Deprecated
    public void connect() throws IOException {
        connect("localhost", 9092);
    }

    public void connect(String host, int port) throws IOException {
        Properties producerProperties = loadProperties("kafka-producer.properties", host, port);
        this.producer = new KafkaProducer<>(producerProperties);

        Properties consumerProperties = loadProperties("kafka-consumer.properties", host, port);
        consumerProperties.put("group.id", uuid);
        this.consumer = new KafkaConsumer<>(consumerProperties);

        this.pollingThread.start(this.consumer);
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
        producer.send(new ProducerRecord<>(TOPICS[UPDATE_STREAM], 0, update));
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

}
