package eu.modernmt.core.cluster.storage.rsync;

import eu.modernmt.core.cluster.storage.DirectorySynchronizer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Created by davide on 13/01/16.
 */
class RSyncSynchronizer implements DirectorySynchronizer {

    private final Logger logger = LogManager.getLogger(RSyncSynchronizer.class);

    @Override
    public void synchronize(InetAddress host, int port, File localPath) throws IOException {
        File passwd = File.createTempFile("mmtrsync", ".passwd");
        FileUtils.write(passwd, RSyncStorage.RSYNC_PASSWD, false);

        String remotePath = RSyncStorage.RSYNC_USER + '@' + host.getHostAddress() + "::engine/";

        String[] command = new String[]{
                "rsync", "-Wau", remotePath, localPath.getAbsolutePath(),
                "--port", Integer.toString(port), "--password-file", passwd.getAbsolutePath()
        };

        logger.info("Running command: " + Arrays.toString(command));

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);

        int code;
        try {
            code = process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException("Process interrupted", e);
        }

        if (code != 0)
            throw new IOException("rsync command exit with code " + code + ": " + IOUtils.toString(process.getErrorStream()));
    }

}
