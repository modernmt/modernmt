package eu.modernmt.engine;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by davide on 13/01/16.
 */
public class EngineSynchronizer {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private MMTWorker.MasterHost master;
    private String remotePath;
    private File localPath;

    public EngineSynchronizer(MMTWorker.MasterHost master, File localPath, String remotePath) {
        this.master = master;
        this.localPath = localPath;
        this.remotePath = remotePath;
    }

    public void sync() throws IOException {
        if (master == null) {
            File remoteFile = new File(remotePath).getAbsoluteFile();
            File localFile = localPath.getAbsoluteFile();

            if (remoteFile.equals(localFile))
                return;
        }

        rsync();
    }

    private String[] getRSyncCommand() {
        ArrayList<String> command = new ArrayList<>(6);
        command.add("rsync");
        command.add("-Wau");

        if (master != null && master.pem != null) {
            command.add("-e");
            command.add("ssh -i " + master.pem);
        }

        String pathPrefix = "";
        if (master != null)
            pathPrefix = master.user + '@' + master.host + ':';
        command.add(pathPrefix + remotePath + File.separatorChar);

        command.add(localPath.getAbsolutePath());

        return command.toArray(new String[command.size()]);
    }

    private void rsync() throws IOException {
        String[] command = getRSyncCommand();
        String[] envp = null;

        if (master.pem == null && master.password != null) {
            envp = new String[]{"RSYNC_PASSWORD=" + master.password};
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

}
