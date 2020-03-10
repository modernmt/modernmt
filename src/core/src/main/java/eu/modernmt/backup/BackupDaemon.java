package eu.modernmt.backup;

import eu.modernmt.config.NodeConfig;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.io.FileConst;
import eu.modernmt.io.Paths;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BackupDaemon implements Closeable {

    private final Logger logger = LogManager.getLogger(BackupDaemon.class);

    private final RetentionPolicy policy;
    private final BackupEngine engine;
    private final File backups;

    public BackupDaemon(NodeConfig config, RetentionPolicy policy) {
        this.policy = policy;

        File engineRoot = FileConst.getEngineRoot(config.getEngineConfig().getName());
        this.engine = new BackupEngine(config, new File(engineRoot, "models"));
        this.backups = new File(engineRoot, "backups");
    }

    public synchronized void start() throws BootstrapException {
        this.engine.start();
    }

    public synchronized void backup() throws IOException, BootstrapException {
        BackupFile currentBackup = BackupFile.create(this.backups);

        // Stop the engine
        this.engine.stop(true);

        // Starting actual backup operation
        logger.info("Creating backup: " + currentBackup);
        long begin = System.currentTimeMillis();

        // Copy models
        File models = this.engine.getModelsPath();
        File context = Paths.join(models, "context");
        File memory = Paths.join(models, "decoder", "memory");

        FileUtils.copyDirectory(context, Paths.join(currentBackup.getPath(), "context"), true);
        FileUtils.copyDirectory(memory, Paths.join(currentBackup.getPath(), "memory"), true);

        // Delete old backups
        List<BackupFile> allBackups = BackupFile.list(backups);
        Set<BackupFile> retainBackups = policy.retain(allBackups);

        for (BackupFile backup : allBackups) {
            if (!retainBackups.contains(backup)) {
                logger.info("Deleting old backup: " + backup);
                FileUtils.forceDelete(backup.getPath());
            }
        }

        long elapsed = System.currentTimeMillis() - begin;
        logger.info("BackupFile created in " + (elapsed / 1000.) + "s");

        // Restart the engine
        this.engine.start();
    }

    public Map<Short, Long> getChannelsPositions() {
        return engine.getChannelsPositions();
    }

    @Override
    public synchronized void close() throws IOException {
        this.engine.stop(false);
    }

}