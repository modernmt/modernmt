package eu.modernmt.persistence.mysql.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by andrea on 26/04/17.
 */
public class SQLUtils {
    private static final Logger logger = LogManager.getLogger(SQLUtils.class);

    public static void closeQuietly(Connection connection) {
        try {
            if (connection != null)
                connection.close();
        } catch (Exception e) {
            //do nothing
        }
    }

    public static void closeQuietly(Statement statement) {
        try {
            if (statement != null)
                statement.close();
        } catch (Exception e) {
            //do nothing
        }
    }

    public static void closeQuietly(ResultSet result) {
        try {
            if (result != null)
                result.close();
        } catch (Exception e) {
            //do nothing
        }
    }


    public static void tryCommit(Connection connection) {
        try {
            connection.commit();
        } catch (SQLException e) {
            logger.error("COULD NOT COMMIT THE TRANSACTION " + connection);
            //do nothing
        }
    }

    public static void tryRollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            logger.error("COULD NOT ROLLBACK THE TRANSACTION " + connection + ";");
            //do nothing
        }
    }

    public static void trySetAutocommit(Connection connection, boolean value) {
        try {
            connection.setAutoCommit(value);
        } catch (SQLException e) {
            logger.error("Error while setting autocommit to " + value);
            //do nothing
        }
    }
}
