package eu.modernmt.backup;

import eu.modernmt.config.NodeConfig;
import eu.modernmt.io.FileConst;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class BackupDaemon implements Closeable {

    private final Logger logger = LogManager.getLogger(BackupDaemon.class);

    private final SynchronousQueue<Object> shutdown = new SynchronousQueue<>();
    private final RetentionPolicy policy;
    private final long interval;
    private final BackupEngine engine;
    private final File backups;

    public BackupDaemon(NodeConfig config, RetentionPolicy policy, long interval) {
        this.policy = policy;
        this.interval = interval;

        File engineRoot = FileConst.getEngineRoot(config.getEngineConfig().getName());
        this.engine = new BackupEngine(config, new File(engineRoot, "models"));
        this.backups = new File(engineRoot, "backups");
    }

    public void runForever() throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(this::interrupt));

        long nextBackup = System.currentTimeMillis();

        while (true) {
            // Start the engine
            this.engine.start();

            // Await next backup
            nextBackup += this.interval;
            long awaitFor = nextBackup - System.currentTimeMillis();

            if (awaitFor > 0) {
                logger.info("Next backup in " + (int) (awaitFor / 1000.) + "s");
                Object object = null;
                try {
                    object = shutdown.poll(awaitFor, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    // Ignore it
                }

                if (object != null)
                    break;
            }

            // Drop a backup
            this.engine.stop(true);
            this.backup();
        }
    }

    private void backup() throws IOException {
        Backup currentBackup = Backup.create(this.backups);

        logger.info("Creating backup: " + currentBackup);
        long begin = System.currentTimeMillis();

        // Copy models
        FileUtils.copyDirectory(this.engine.getModelsPath(), currentBackup.getPath(), true);

        // Delete old backups
        List<Backup> allBackups = Backup.list(backups);
        Set<Backup> retainBackups = policy.retain(allBackups);

        for (Backup backup : allBackups) {
            if (!retainBackups.contains(backup)) {
                logger.info("Deleting old backup: " + backup);
                FileUtils.forceDelete(backup.getPath());
            }
        }

        long elapsed = System.currentTimeMillis() - begin;
        logger.info("Backup created in " + (elapsed / 1000.) + "s");
    }

    public void interrupt() {
        try {
            shutdown.put(new Object());
        } catch (InterruptedException e) {
            // Ignore it
        }
    }

    @Override
    public void close() throws IOException {
        this.engine.stop(false);
    }
}