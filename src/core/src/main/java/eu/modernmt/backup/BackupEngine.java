package eu.modernmt.backup;

import eu.modernmt.backup.storage.CorporaBackupStorage;
import eu.modernmt.cluster.kafka.KafkaDataManager;
import eu.modernmt.config.DataStreamConfig;
import eu.modernmt.config.NodeConfig;
import eu.modernmt.context.lucene.LuceneAnalyzer;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.HostUnreachableException;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.hw.NetworkUtils;
import eu.modernmt.io.Paths;
import eu.modernmt.processing.Preprocessor;
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

    private LuceneAnalyzer contextAnalyzer = null;
    private LuceneTranslationMemory memory = null;
    private DataManager dataManager = null;
    private CorporaBackupStorage storage = null;

    public BackupEngine(NodeConfig config, File models) {
        this.uuid = UUID.randomUUID().toString();
        this.config = config;
        this.models = models;

        DataStreamConfig dataStreamConfig = config.getDataStreamConfig();

        boolean localDatastream = NetworkUtils.isLocalhost(dataStreamConfig.getHost());
        boolean embeddedDatastream = dataStreamConfig.isEmbedded();

        if (embeddedDatastream && localDatastream) {
            String host = NetworkUtils.getMyIpv4Address();
            dataStreamConfig.setHost(host);
        }
    }

    public File getModelsPath() {
        return models;
    }

    public void start() throws IOException {
        FileUtils.forceMkdir(this.models);

        logger.info("Starting backup engine...");
        long begin = System.currentTimeMillis();

        contextAnalyzer = new LuceneAnalyzer(Paths.join(models, "context"));
        memory = new LuceneTranslationMemory(Paths.join(models, "memory"), 1);
        storage = new CorporaBackupStorage(Paths.join(models, "storage"));

        dataManager = new KafkaDataManager(config.getEngineConfig().getLanguageIndex(), new Preprocessor(), null, uuid, config.getDataStreamConfig());
        for (DataListener listener : contextAnalyzer.getDataListeners())
            dataManager.addDataListener(listener);
        dataManager.addDataListener(memory);
        dataManager.addDataListener(storage);

        Map<Short, Long> positions;
        try {
            positions = dataManager.connect(60, TimeUnit.SECONDS, true, false);
        } catch (HostUnreachableException e) {
            throw new IOException(e);
        }

        long elapsed = System.currentTimeMillis() - begin;
        logger.info("Backup engine started in " + (elapsed / 1000.) + "s, channels: " + positions);
    }

    public void stop() throws IOException {
        this.stop(true);
    }

    public void stop(boolean optimize) throws IOException {
        IOException dmError = close(dataManager);
        IOException caError;
        IOException mError;
        IOException sError;

        try {
            if (optimize) {
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

                logger.info("Running optimization for Storage...");
                begin = System.currentTimeMillis();
                storage.optimize();
                elapsed = System.currentTimeMillis() - begin;
                logger.info("Optimization for Storage completed in " + (elapsed / 1000.) + "s");
            }
        } finally {
            caError = close(contextAnalyzer);
            mError = close(memory);
            sError = close(storage);
        }

        if (dmError != null)
            throw dmError;
        if (caError != null)
            throw caError;
        if (mError != null)
            throw mError;
        if (sError != null)
            throw sError;

        dataManager = null;
        contextAnalyzer = null;
        memory = null;
        storage = null;

        logger.info("Backup engine stopped");
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
