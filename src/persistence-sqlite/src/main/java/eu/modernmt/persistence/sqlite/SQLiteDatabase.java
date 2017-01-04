package eu.modernmt.persistence.sqlite;

import eu.modernmt.persistence.*;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by davide on 21/09/16.
 */
public class SQLiteDatabase extends Database {

    public SQLiteDatabase(File database) throws PersistenceException {
        super("jdbc:sqlite:" + database, null, null);

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new PersistenceException("Failed to load SQLite JDBC driver", e);
        }
    }

    @Override
    public Connection getConnection(boolean cached) throws PersistenceException {
        return new SQLiteConnection(connectionString);
    }

    @Override
    public DomainDAO getDomainDAO(Connection connection) {
        return new SQLiteDomainDAO((SQLiteConnection) connection);
    }

    @Override
    public ImportJobDAO getImportJobDAO(Connection connection) {
        return new SQLiteImportJobDAO((SQLiteConnection) connection);
    }

    @Override
    public void drop(Connection wconnection) throws PersistenceException {
        java.sql.Connection connection = ((SQLiteConnection) wconnection).connection;
        Statement statement = null;

        try {
            statement = connection.createStatement();
            statement.executeUpdate("DROP TABLE IF EXISTS domains");
            statement.executeUpdate("DROP TABLE IF EXISTS import_jobs");
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
        }
    }

    @Override
    public void create(Connection wconnection) throws PersistenceException {
        java.sql.Connection connection = ((SQLiteConnection) wconnection).connection;
        Statement statement = null;

        try {
            statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE domains (\"id\" INTEGER PRIMARY KEY AUTOINCREMENT, \"name\" TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE import_jobs (\"id\" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "\"domain\" INTEGER NOT NULL, " +
                    "\"size\" INTEGER, " +
                    "\"data_channel\" INTEGER, " +
                    "\"begin\" INTEGER NOT NULL, " +
                    "\"end\" INTEGER NOT NULL)");
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
        }
    }

}
