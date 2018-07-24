package eu.modernmt.persistence.mysql;

import com.mysql.cj.jdbc.MysqlDataSource;
import eu.modernmt.config.DatabaseConfig;
import eu.modernmt.persistence.*;
import eu.modernmt.persistence.mysql.utils.SQLUtils;
import org.apache.commons.io.IOUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by andrea on 25/04/17.
 */
public class MySQLDatabase extends Database {

    private String name;
    private DataSource dataSource;

    public MySQLDatabase(DatabaseConfig config) {
        this(config.getHost(), config.getPort(), config.getName(), config.getUser(), config.getPassword());
    }

    public MySQLDatabase(String host, int port, String name, String user, String password) {
        super(null);
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
    public void create() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public void close() {
        //do nothing
    }

    @Override
    public String getName() {
        return this.name;
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