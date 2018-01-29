package eu.modernmt.persistence.mysql;

import com.mysql.cj.jdbc.MysqlDataSource;
import eu.modernmt.persistence.*;
import eu.modernmt.persistence.mysql.utils.SQLUtils;
import org.apache.commons.io.IOUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by andrea on 25/04/17.
 */
public class MySQLDatabase extends Database {

    private String name;
    private DataSource dataSource;

    public MySQLDatabase(String host, int port, String name, String user, String password) {
        this.name = name;

        String params = "useUnicode=true"
                + "&useJDBCCompliantTimezoneShift=true"
                + "&useLegacyDatetimeCode=false"
                + "&serverTimezone=UTC";

        MysqlDataSource mysqlDS = new MysqlDataSource();
        mysqlDS.setURL("jdbc:mysql://" + host + ":" + port + "/" + name + "?" + params);
        mysqlDS.setDatabaseName(name);
        mysqlDS.setUser(user);
        mysqlDS.setPassword(password);
        this.dataSource = mysqlDS;
    }

    /**
     * This method provides a connection to the MySQL DB
     *
     * @param cached
     * @return A Connection object, that can be used to communicate with the DB
     * @throws PersistenceException
     */
    @Override
    public MySQLConnection getConnection(boolean cached) throws PersistenceException {
        try {
            return new MySQLConnection(dataSource.getConnection());
        } catch (SQLException e) {
            throw new PersistenceException("SQLException: unable to connect" + e);
        }
    }

    @Override
    public MemoryDAO getMemoryDAO(Connection connection) {
        return new MySQLMemoryDAO((MySQLConnection) connection);
    }

    @Override
    public ImportJobDAO getImportJobDAO(Connection connection) {
        return new MySQLImportJobDAO((MySQLConnection) connection);
    }

    @Override
    public void create() throws PersistenceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists() throws PersistenceException {
        return true;
    }

    @Override
    public void close() throws IOException {
        //do nothing
    }

    @Override
    public String getName() throws PersistenceException {
        return this.name;
    }

    @Override
    public boolean initialize() throws PersistenceException {
        java.sql.Connection connection = null;
        PreparedStatement statement = null;
        try {
            String query = "UPDATE mmt_metadata SET initialized = ? WHERE id = ? AND initialized = ?";
            connection = this.dataSource.getConnection();
            statement = connection.prepareStatement(query);
            statement.setShort(1, (short) 1);
            statement.setLong(2, 1L);
            statement.setShort(3, (short) 0);
            int affectedRows = statement.executeUpdate();
            return affectedRows != 0;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(connection);
            SQLUtils.closeQuietly(statement);
        }
    }

    @Override
    public void testConnection() throws PersistenceException {
        MySQLConnection connection = null;
        Statement statement = null;
        ResultSet result = null;

        try {
            connection = getConnection(true);
            statement = connection.getDataSourceConnection().createStatement();
            result = statement.executeQuery("SELECT 1");
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
            SQLUtils.closeQuietly(result);
            IOUtils.closeQuietly(connection);
        }
    }
}