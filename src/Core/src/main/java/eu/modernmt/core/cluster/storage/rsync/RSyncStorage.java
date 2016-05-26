package eu.modernmt.core.cluster.storage.rsync;

import eu.modernmt.core.Engine;
import eu.modernmt.core.cluster.storage.DirectorySynchronizer;
import eu.modernmt.core.cluster.storage.StorageService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 26/05/16.
 */
public class RSyncStorage extends StorageService implements AutoCloseable {

    public static final String RSYNC_USER = "mmtrsync";
    public static final String RSYNC_PASSWD = "9i2p9#uu7A!9a";

    private DirectorySynchronizer synchronizer = new RSyncSynchronizer();
    private Process process = null;

    @Override
    public synchronized void start(int port, Engine engine) throws IOException {
        if (process != null)
            throw new IllegalStateException("StorageService already started");

        // Writing config files
        File folder = engine.getRuntimeFolder("rsync", true);
        File configFile = new File(folder, "rsyncd.config");
        File secretsFile = new File(folder, "rsyncd.secrets");

        FileUtils.write(secretsFile, RSYNC_USER + ":" + RSYNC_PASSWD, false);

        Properties config = new Properties();
        config.setProperty("path", engine.getRootPath().getAbsolutePath());
        config.setProperty("read only", "true");
        config.setProperty("use chroot", "false");
        config.setProperty("list", "no");
        config.setProperty("auth users", RSYNC_USER);
        config.setProperty("secrets file", secretsFile.getAbsolutePath());

        FileOutputStream output = null;

        try {
            output = new FileOutputStream(configFile, false);
            output.write("[engine]\n".getBytes());
            config.store(output, null);
        } finally {
            IOUtils.closeQuietly(output);
        }

        // Starting process
        String[] command = new String[]{
                "rsync", "--daemon", "--config", configFile.getAbsolutePath(), "--port", Integer.toString(port), "--no-detach"
        };

        Runtime runtime = Runtime.getRuntime();
        process = runtime.exec(command);
    }

    @Override
    public DirectorySynchronizer getDirectorySynchronizer() {
        return synchronizer;
    }

    @Override
    public void close() throws IOException {
        if (process != null) {
            process.destroy();

            try {
                if (!process.waitFor(1, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
            }

            process = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }
}
