package eu.modernmt.cluster.storage;

import eu.modernmt.cluster.storage.rsync.RSyncStorage;
import eu.modernmt.engine.Engine;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by davide on 26/05/16.
 */
public abstract class StorageService implements Closeable {

    private static StorageService instance = null;

    public static StorageService getInstance() {
        if (instance == null)
            instance = new RSyncStorage();

        return instance;
    }

    public abstract void start(int port, Engine engine) throws IOException;

    public abstract DirectorySynchronizer getDirectorySynchronizer();

}
