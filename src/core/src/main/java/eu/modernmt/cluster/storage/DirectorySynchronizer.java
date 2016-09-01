package eu.modernmt.cluster.storage;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by davide on 22/04/16.
 */
public interface DirectorySynchronizer {

    void synchronize(InetAddress host, int port, File localPath) throws IOException;

}
