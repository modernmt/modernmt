package eu.modernmt.cluster.kafka;

import eu.modernmt.cluster.EmbeddedService;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.io.FileConst;
import eu.modernmt.io.DefaultCharset;
import eu.modernmt.io.NetworkUtils;
import eu.modernmt.io.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 07/04/17.
 */
public class EmbeddedKafka extends EmbeddedService {

    public static EmbeddedKafka start(Engine engine, int port) throws BootstrapException {
        try {
            EmbeddedKafka instance = new EmbeddedKafka(engine);
            instance.start(port);

            return instance;
        } catch (IOException e) {
            throw new BootstrapException(e);
        }
    }

    private final File data;
    private final File runtime;
    private final File logFile;
    private final File kafkaBin;
    private final File zookeeperBin;

    private EmbeddedKafka(Engine engine) throws IOException {
        this.data = new File(engine.getModelsPath(), "kafka");
        this.runtime = engine.getRuntimeFolder("kafka", true);
        this.logFile = engine.getLogFile("embedded-kafka.log");

        File kafkaHome = Paths.join(FileConst.getVendorPath(), "kafka-0.10.0.1");

        this.kafkaBin = Paths.join(kafkaHome, "bin", "kafka-server-start.sh");
        this.zookeeperBin = Paths.join(kafkaHome, "bin", "zookeeper-server-start.sh");
    }

    private void start(int port) throws IOException {
        if (!NetworkUtils.isAvailable(port))
            throw new IOException("Port " + port + " is already in use by another process");

        FileUtils.deleteDirectory(this.runtime);
        FileUtils.forceMkdir(this.runtime);

        FileUtils.deleteQuietly(this.logFile);
        FileUtils.touch(this.logFile);

        Process zookeeper = null;
        Process kafka;

        boolean success = false;

        try {
            int zookeperPort = NetworkUtils.getAvailablePort();

            zookeeper = this.startZookeeper(zookeperPort);
            kafka = this.startKafka(port, zookeperPort);

            success = true;
        } finally {
            if (!success)
                this.kill(zookeeper, 1, TimeUnit.SECONDS);
        }

        this.subprocesses = Arrays.asList(kafka, zookeeper);
    }

    private Process startZookeeper(int port) throws IOException {
        File zdata = new File(this.runtime, "zookeeper_data");
        FileUtils.forceMkdir(zdata);

        Properties properties = new Properties();
        properties.setProperty("dataDir", zdata.getAbsolutePath());
        properties.setProperty("clientPort", Integer.toString(port));
        properties.setProperty("maxClientCnxns", "0");

        File config = new File(this.runtime, "zookeeper.properties");
        write(properties, config);

        ProcessBuilder builder = new ProcessBuilder(this.zookeeperBin.getAbsolutePath(), config.getAbsolutePath());
        builder.redirectError(ProcessBuilder.Redirect.appendTo(this.logFile));
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(this.logFile));

        Process zookeeper = builder.start();
        boolean success = false;

        try {
            for (int i = 0; i < 20; i++) {
                if (!zookeeper.isAlive())
                    throw new IOException("Unable to start Zookeeper process, more details here: " + this.logFile.getAbsolutePath());

                NetworkUtils.NetPipe pipe = null;
                try {
                    pipe = NetworkUtils.netcat("localhost", port);
                    pipe.send("ruok");

                    if ("imok".equals(pipe.receive(2, TimeUnit.SECONDS))) {
                        success = true;
                        return zookeeper;
                    }
                } catch (IOException e) {
                    // Ignore
                } finally {
                    IOUtils.closeQuietly(pipe);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new IOException("Unexpected interruption", e);
                }
            }

            throw new IOException("Zookeeper process startup timeout, more details here: " + this.logFile.getAbsolutePath());
        } finally {
            if (!success)
                zookeeper.destroyForcibly();
        }
    }

    private Process startKafka(int port, int zookeperPort) throws IOException {
        if (!this.data.isDirectory())
            FileUtils.forceMkdir(this.data);

        Properties properties = new Properties();
        properties.setProperty("broker.id", "0");
        properties.setProperty("listeners", "PLAINTEXT://:" + port);
        properties.setProperty("log.dirs", this.data.getAbsolutePath());
        properties.setProperty("num.partitions", "1");
        properties.setProperty("log.retention.hours", "8760000");
        properties.setProperty("zookeeper.connect", "localhost:" + zookeperPort);

        File config = new File(this.runtime, "kafka.properties");
        write(properties, config);

        ProcessBuilder builder = new ProcessBuilder(this.kafkaBin.getAbsolutePath(), config.getAbsolutePath());
        builder.redirectError(ProcessBuilder.Redirect.appendTo(this.logFile));
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(this.logFile));

        Process kafka = builder.start();
        boolean success = false;

        try {
            for (int i = 0; i < 20; i++) {
                if (!kafka.isAlive())
                    throw new IOException("Unable to start Kafka process, more details here: " + this.logFile.getAbsolutePath());

                LineIterator lines = FileUtils.lineIterator(this.logFile, DefaultCharset.get().name());

                while (lines.hasNext()) {
                    String line = lines.next();

                    if (line.contains("INFO [Kafka Server 0], started (kafka.server.KafkaServer)")) {
                        success = true;
                        return kafka;
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new IOException("Unexpected interruption", e);
                }
            }

            throw new IOException("Kafka process startup timeout, more details here: " + this.logFile.getAbsolutePath());
        } finally {
            if (!success)
                kafka.destroyForcibly();
        }
    }

    private static void write(Properties properties, File dest) throws IOException {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(dest, false);
            properties.store(output, null);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    @Override
    public void shutdown() {
        this.kill(this.subprocesses.get(0), 5, TimeUnit.SECONDS); // kafka
        this.kill(this.subprocesses.get(1), 2, TimeUnit.SECONDS); // zookeeper
    }
}
