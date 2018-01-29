package eu.modernmt.persistence;

import java.io.Closeable;

/**
 * Created by davide on 21/09/16.
 */
public abstract class Database implements Closeable {

    public final Connection getConnection() throws PersistenceException {
        return getConnection(true);
    }

    public abstract Connection getConnection(boolean cached) throws PersistenceException;

    public abstract MemoryDAO getMemoryDAO(Connection connection);

    public abstract ImportJobDAO getImportJobDAO(Connection connection);

    public abstract void create() throws PersistenceException;

    public abstract boolean exists() throws PersistenceException;

    public abstract String getName() throws PersistenceException;

    public abstract boolean initialize() throws PersistenceException;

    public abstract void testConnection() throws PersistenceException;

}
