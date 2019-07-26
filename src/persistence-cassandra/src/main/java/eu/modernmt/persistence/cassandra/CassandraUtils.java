package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.*;
import eu.modernmt.persistence.PersistenceException;

/**
 * CassandraUtils offers a series of static methods
 * for the queue of queries and statements,
 * making sure that any possible exception thrown
 * during the interaction with Cassandra
 * is caught and handled.
 */
public class CassandraUtils {

    /**
     * This method performs the queue of a statement
     * and checks for all the possible exceptions that it may throw
     *
     * @param connection the current connection with the DB
     * @param statement  the statement to execute
     * @return the ResultSet obtained from the queue of the statement
     * @throws PersistenceException
     */
    public static ResultSet checkedExecute(CassandraConnection connection, Statement statement) throws PersistenceException {
        try {
            return connection.session.execute(statement);
        } catch (DriverException e) {
            throw unwrap(e);
        }
    }

    /**
     * This method performs the queue of a query string
     * and checks for all the possible exceptions that it may throw
     *
     * @param connection the current connection with the DB
     * @param query      the string with the query to execute
     * @return the ResultSet obtained from the queue of the query
     * @throws PersistenceException
     */
    public static ResultSet checkedExecute(CassandraConnection connection, String query) throws PersistenceException {
        try {
            return connection.session.execute(query);
        } catch (DriverException e) {
            throw unwrap(e);
        }
    }


    private static PersistenceException unwrap(DriverException cause) throws PersistenceException {

        if (cause instanceof NoHostAvailableException)
            return new PersistenceException("no host in the cluster could be contacted successfully to execute this query", cause);
        else if (cause instanceof QueryExecutionException)
            return new PersistenceException("Cassandra couldn't execute the query with the requested consistency level successfully", cause);
        else if (cause instanceof QueryValidationException)
            return new PersistenceException("invalid query (possible causes: syntax error, unauthorized...).", cause);
        else if (cause instanceof UnsupportedFeatureException)
            return new PersistenceException("the protocol version 1 is in use and a feature not supported has been used. " +
                    "Features that are not supported by the version protocol 1 include: " +
                    "BatchStatement, ResultSet paging and binary values in RegularStatement",
                    cause);
        else if (cause instanceof OperationTimedOutException)
            return new PersistenceException("Timed out", cause);
        else
            return new PersistenceException("Unexpected exception", cause);

    }

}
