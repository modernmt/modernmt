package eu.modernmt.core.cluster;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by davide on 13/01/16.
 */
public class RSyncSynchronizer implements DirectorySynchronizer {

    private final Logger logger = LogManager.getLogger(RSyncSynchronizer.class);

    private final InetAddress host;
    private final String user;
    private final String password;
    private final File pem;
    private final String remotePath;
    private final File localPath;

    public RSyncSynchronizer(InetAddress host, String user, String password, File localPath, String remotePath) {
        this.host = host;
        this.user = user;
        this.password = password;
        this.pem = null;
        this.localPath = localPath;
        this.remotePath = remotePath;
    }

    public RSyncSynchronizer(InetAddress host, File pem, File localPath, String remotePath) {
        this.host = host;
        this.user = null;
        this.password = null;
        this.pem = pem;
        this.localPath = localPath;
        this.remotePath = remotePath;
    }

    @Override
    public void synchronize() throws IOException {
        if (isLocalAddress(host)) {
            File remoteFile = new File(remotePath).getAbsoluteFile();
            File localFile = localPath.getAbsoluteFile();

            if (!remoteFile.equals(localFile))
                rsync(true);
        } else {
            rsync(false);
        }
    }

    private void rsync(boolean localhost) throws IOException {
        String[] command = getRSyncCommand(localhost);
        String[] envp = null;

        if (pem == null && password != null) {
            envp = new String[]{"RSYNC_PASSWORD=" + password};
        }

        logger.info("Running command: " + Arrays.toString(command));

        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command, envp);

        int code;
        try {
            code = process.waitFor();
        } catch (InterruptedException e) {
            throw new IOException("Process interrupted", e);
        }

        if (code != 0)
            throw new IOException("rsync command exit with code " + code + ": " + IOUtils.toString(process.getErrorStream()));
    }

    private String[] getRSyncCommand(boolean localhost) {
        ArrayList<String> command = new ArrayList<>(6);
        command.add("rsync");
        command.add("-Wau");

        if (!localhost && pem != null) {
            command.add("-e");
            command.add("ssh -i " + pem);
        }

        String pathPrefix = "";
        if (!localhost)
            pathPrefix = user + '@' + host + ':';
        command.add(pathPrefix + remotePath + File.separatorChar);

        command.add(localPath.getAbsolutePath());

        return command.toArray(new String[command.size()]);
    }

    public static boolean isLocalAddress(InetAddress address) {
        // Check if the address is a valid special local or loop back
        if (address.isAnyLocalAddress() || address.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(address) != null;
        } catch (SocketException e) {
            return false;
        }
    }

}
