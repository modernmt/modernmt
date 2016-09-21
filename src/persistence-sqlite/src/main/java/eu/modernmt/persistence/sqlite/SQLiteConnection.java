package eu.modernmt.persistence.sqlite;

import eu.modernmt.persistence.Connection;
import eu.modernmt.persistence.PersistenceException;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Created by davide on 21/09/16.
 */
public class SQLiteConnection implements Connection {

    final java.sql.Connection connection;

    public SQLiteConnection(String connectionString) throws PersistenceException {
        try {
            connection = DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

}
