package eu.modernmt.core.cluster.storage.rsync;

import eu.modernmt.core.Engine;
import eu.modernmt.core.cluster.storage.DirectorySynchronizer;
import eu.modernmt.core.cluster.storage.StorageService;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 26/05/16.
 */
public class RSyncStorage extends StorageService implements AutoCloseable {

    public static final String RSYNC_USER = "mmtrsync";
    public static final String RSYNC_PASSWD = "9i2p9#uu7A!9a";

    private static final String CONFIG_TEMPLATE = "[engine]\n" +
            "path = {path}\n" +
            "read only = true\n" +
            "use chroot = false\n" +
            "list = no\n" +
            "auth users = " + RSYNC_USER + "\n" +
            "secrets file = {secrets}\n";

    private DirectorySynchronizer synchronizer = new RSyncSynchronizer();
    private Process process = null;

    @Override
    public synchronized void start(int port, Engine engine) throws IOException {
        if (process != null)
            throw new IllegalStateException("StorageService already started");

        // Writing config files
        File folder = engine.getRuntimeFolder("rsync", true);
        File secretsFile = writeSecretsFile(folder);
        File configFile = writeConfigFile(folder, engine, secretsFile);

        // Starting process
        String[] command = new String[]{
                "rsync", "--daemon", "--config", configFile.getAbsolutePath(), "--port", Integer.toString(port), "--no-detach"
        };

        Runtime runtime = Runtime.getRuntime();
        process = runtime.exec(command);
    }

    private static File writeSecretsFile(File folder) throws IOException {
        File secretsFile = new File(folder, "rsyncd.secrets");

        FileUtils.write(secretsFile, RSYNC_USER + ":" + RSYNC_PASSWD, false);
        if (!secretsFile.setExecutable(false, false) || !secretsFile.setReadable(false, false)
                || !secretsFile.setWritable(false, false) || !secretsFile.setReadable(true, true))
            throw new IOException("Unable to change file permissions: " + secretsFile);

        return secretsFile;
    }

    private static File writeConfigFile(File folder, Engine engine, File secretsFile) throws IOException {
        File configFile = new File(folder, "rsyncd.config");

        String content = CONFIG_TEMPLATE
                .replace("{path}", engine.getRootPath().getAbsolutePath())
                .replace("{secrets}", secretsFile.getAbsolutePath());

        FileUtils.write(configFile, content);
        return configFile;
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
