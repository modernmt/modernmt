package eu.modernmt.persistence;

/**
 * Created by davide on 21/09/16.
 */
public abstract class Database {

    protected String connectionString;
    protected String username;
    protected String password;

    protected Database(String connectionString, String username, String password) throws PersistenceException {
        this.connectionString = connectionString;
        this.username = username;
        this.password = password;
    }

    public final Connection getConnection() throws PersistenceException {
        return getConnection(true);
    }

    public abstract Connection getConnection(boolean cached) throws PersistenceException;

    public abstract DomainDAO getDomainDAO(Connection connection);

    public abstract void drop(Connection connection) throws PersistenceException;

    public abstract void create(Connection connection) throws PersistenceException;

}
