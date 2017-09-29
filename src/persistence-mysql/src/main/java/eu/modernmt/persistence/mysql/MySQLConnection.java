package eu.modernmt.persistence.mysql;

import eu.modernmt.persistence.Connection;
import eu.modernmt.persistence.PersistenceException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by andrea on 29/09/17.
 * A MySQLConnection object represents a connection with a MySQLConnection dataBase.
 */
public class MySQLConnection implements Connection {
    private java.sql.Connection dataSourceconnection;

    public MySQLConnection(java.sql.Connection connection) throws PersistenceException {
        this.dataSourceconnection = connection;
    }

    public java.sql.Connection getDataSourceConnection() {
        return this.dataSourceconnection;
    }

    /**
     * This method closes the connection with the current DB
     */
    @Override
    public void close() throws IOException {
        try {
            this.dataSourceconnection.close();
        } catch (SQLException e) {
            throw new IOException("Error while closing DB connection");
        }
    }

}
