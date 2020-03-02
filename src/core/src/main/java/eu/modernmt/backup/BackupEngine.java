package eu.modernmt.backup;

import eu.modernmt.cluster.kafka.KafkaBinaryLog;
import eu.modernmt.config.BinaryLogConfig;
import eu.modernmt.config.NodeConfig;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.data.BinaryLog;
import eu.modernmt.data.HostUnreachableException;
import eu.modernmt.data.LogDataListener;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.hw.NetworkUtils;
import eu.modernmt.memory.TranslationMemory;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BackupEngine {

    private final Logger logger = LogManager.getLogger(BackupEngine.class);

    private final String uuid;
    private final NodeConfig config;
    private final File models;

    private Engine engine = null;
    private BinaryLog binlog = null;

    public BackupEngine(NodeConfig config, File models) {
        this.uuid = UUID.randomUUID().toString();
        this.config = config;
        this.models = models;

        BinaryLogConfig binaryLogConfig = config.getBinaryLogConfig();

        String[] hosts = binaryLogConfig.getHosts();
        boolean localBinaryLog = hosts.length == 1 && NetworkUtils.isLocalhost(hosts[0]);
        boolean embeddedBinaryLog = binaryLogConfig.isEmbedded();

        if (embeddedBinaryLog && localBinaryLog) {
            String host = NetworkUtils.getMyIpv4Address();
            binaryLogConfig.setHost(host);
        }
    }

    public File getModelsPath() {
        return models;
    }

    public void start() throws BootstrapException {
        try {
            FileUtils.forceMkdir(this.models);
        } catch (IOException e) {
            throw new BootstrapException(e);
        }

        logger.info("Starting backup engine...");
        long begin = System.currentTimeMillis();

        engine = Engine.load(config.getEngineConfig());
        binlog = new KafkaBinaryLog(engine, uuid, config.getBinaryLogConfig());

        for (LogDataListener listener : engine.getDataListeners())
            binlog.addLogDataListener(listener);

        Map<Short, Long> positions;
        try {
            positions = binlog.connect(60, TimeUnit.SECONDS, true, false);
        } catch (HostUnreachableException e) {
            throw new BootstrapException(e);
        }

        long elapsed = System.currentTimeMillis() - begin;
        logger.info("BackupFile engine started in " + (elapsed / 1000.) + "s, channels: " + positions);
    }

    public void stop() throws IOException {
        this.stop(true);
    }

    public void stop(boolean optimize) throws IOException {
        IOException binlogError = close(binlog);
        IOException engineError;

        try {
            if (optimize) {
                ContextAnalyzer contextAnalyzer = this.engine.getContextAnalyzer();
                TranslationMemory memory = this.engine.getDecoder().getTranslationMemory();

                long begin, elapsed;

                logger.info("Running optimization for Context Analyzer...");
                begin = System.currentTimeMillis();
                contextAnalyzer.optimize();
                elapsed = System.currentTimeMillis() - begin;
                logger.info("Optimization for Context Analyzer completed in " + (elapsed / 1000.) + "s");

                logger.info("Running optimization for Memory...");
                begin = System.currentTimeMillis();
                memory.optimize();
                elapsed = System.currentTimeMillis() - begin;
                logger.info("Optimization for Memory completed in " + (elapsed / 1000.) + "s");
            }
        } catch (ContextAnalyzerException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException)
                throw (IOException) cause;
            else
                throw new IOException(e);
        } finally {
            engineError = close(engine);
        }

        if (binlogError != null)
            throw binlogError;
        if (engineError != null)
            throw engineError;

        binlog = null;
        engine = null;

        logger.info("BackupFile engine stopped");
    }


    private IOException close(Closeable closeable) {
        try {
            closeable.close();
            return null;
        } catch (IOException e) {
            logger.error(e);
            return e;
        }
    }

}
