package eu.modernmt.cluster;

import eu.modernmt.cluster.kafka.KafkaDataManager;
import eu.modernmt.config.NodeConfig;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.lucene.LuceneAnalyzer;
import eu.modernmt.data.DataManager;
import eu.modernmt.decoder.neural.memory.TranslationMemory;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.io.Paths;
import eu.modernmt.processing.Preprocessor;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class BackupDaemon implements Closeable {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private final Logger logger = LogManager.getLogger(BackupDaemon.class);

    private final SynchronousQueue<Object> shutdown = new SynchronousQueue<>();
    private final File folder;
    private final File models;
    private final int limit;

    private NodeConfig config = null;

    private ContextAnalyzer contextAnalyzer = null;
    private TranslationMemory memory = null;
    private DataManager dataManager = null;

    public BackupDaemon(File folder, int limit) {
        this.folder = folder;
        this.models = new File(folder, "models");
        this.limit = limit;
    }

    public void runForever(NodeConfig config, long time) throws Throwable {
        this.config = config;
        this.startComponents();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown.put(new Object());
            } catch (InterruptedException e) {
                // ignore it
            }
        }));

        long nextBackup = System.currentTimeMillis();

        while (true) {
            nextBackup += time;

            long awaitFor = nextBackup - System.currentTimeMillis();

            if (awaitFor > 0) {
                Object object = shutdown.poll(awaitFor, TimeUnit.MILLISECONDS);
                if (object != null)
                    break;
            }

            backup();
        }
    }

    private void backup() throws Throwable {
        this.dataManager.close();
        this.dataManager = null;

        this.memory.optimize();
        this.contextAnalyzer.optimize();

        this.close();
        this.createBackup();
        this.startComponents();
    }

    private void createBackup() throws IOException {
        File outputFolder = new File(this.folder, "models-" + dateFormat.format(new Date()));
        FileUtils.copyDirectory(this.models, outputFolder, true);

        File[] backups = this.folder.listFiles((dir, name) -> name.startsWith("models-"));
        if (backups == null)
            throw new IOException("backup list is null for folder " + this.folder);

        if (backups.length > limit) {
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < backups.length - limit; i++)
                FileUtils.deleteDirectory(backups[i]);
        }
    }

    private void startComponents() throws Throwable {
        close();

        long begin = System.currentTimeMillis();

        contextAnalyzer = new LuceneAnalyzer(Paths.join(models, "context"));
        memory = new LuceneTranslationMemory(Paths.join(models, "memory"), 1);

        dataManager = new KafkaDataManager(config.getEngineConfig().getLanguageIndex(), new Preprocessor(), null, UUID.randomUUID().toString(), config.getDataStreamConfig());
        dataManager.addDataListener(contextAnalyzer);
        dataManager.addDataListener(memory);

        Map<Short, Long> positions = dataManager.connect();

        long elapsed = System.currentTimeMillis() - begin;
        logger.info("Components started in " + (elapsed / 1000.) + "s, channels: " + positions);
    }

    @Override
    public void close() throws IOException {
        IOException error = null;

        if (dataManager != null) {
            try {
                dataManager.close();
                dataManager = null;
            } catch (IOException e) {
                error = e;
            }
        }

        if (contextAnalyzer != null) {
            try {
                contextAnalyzer.close();
                contextAnalyzer = null;
            } catch (IOException e) {
                error = e;
            }
        }

        if (memory != null) {
            try {
                memory.close();
                memory = null;
            } catch (IOException e) {
                error = e;
            }
        }

        if (error != null)
            throw error;
    }
}
