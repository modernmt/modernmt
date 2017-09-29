package eu.modernmt.persistence.mysql;

import com.mysql.cj.jdbc.MysqlDataSource;
import eu.modernmt.config.DatabaseConfig;
import eu.modernmt.persistence.*;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Created by andrea on 25/04/17.
 */
public class MySQLDatabase extends Database {
    private String name;
    private DataSource dataSource;

    public MySQLDatabase(DatabaseConfig config) {
        this.name = config.getName();

        String params = "useUnicode=true"
                + "&useJDBCCompliantTimezoneShift=true"
                + "&useLegacyDatetimeCode=false"
                + "&serverTimezone=UTC";

        MysqlDataSource mysqlDS = new MysqlDataSource();
        mysqlDS.setURL(config.getHost() + ":" + config.getPort() + "?" + params);
        mysqlDS.setDatabaseName(config.getName());
        mysqlDS.setUser(config.getUser());
        mysqlDS.setPassword(config.getPassword());
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
    public Connection getConnection(boolean cached) throws PersistenceException {
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
    public String getName() throws PersistenceException {
        return this.name;
    }
}
