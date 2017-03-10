package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.exceptions.*;
import eu.modernmt.persistence.PersistenceException;

/**
 * Created by andrea on 08/03/17.
 */
public class CassandraUtils {

    public static ResultSet checkedExecute(Session session, Statement statement) throws PersistenceException {
        try {
            return session.execute(statement);
        } catch (NoHostAvailableException e) {
            throw new PersistenceException("no host in the cluster could be contacted successfully to execute this query", e);
        } catch (QueryExecutionException e) {
            throw new PersistenceException("Cassandra couldn't execute the query with the requested consistency level successfully", e);
        } catch (QueryValidationException e) {
            throw new PersistenceException("invalid query (possible causes: syntax error, unauthorized...).", e);
        } catch (UnsupportedFeatureException e) {
            throw new PersistenceException("the protocol version 1 is in use and a feature not supported has been used. " +
                    "Features that are not supported by the version protocol 1 include: " +
                    "BatchStatement, ResultSet paging and binary values in RegularStatement",
                    e);
        } catch (OperationTimedOutException e) {
            throw new PersistenceException("Timed out",
                    e);
        }
    }

    public static ResultSet checkedExecute(Session session, String statement) throws PersistenceException {
        try {
            return session.execute(statement);
        } catch (NoHostAvailableException e) {
            throw new PersistenceException("no host in the cluster could be contacted successfully to execute this query", e);
        } catch (QueryExecutionException e) {
            throw new PersistenceException("Cassandra couldn't execute the query with the requested consistency level successfully", e);
        } catch (QueryValidationException e) {
            throw new PersistenceException("invalid query (possible causes: syntax error, unauthorized...).", e);
        } catch (UnsupportedFeatureException e) {
            throw new PersistenceException("the protocol version 1 is in use and a feature not supported has been used. " +
                    "Features that are not supported by the version protocol 1 include: " +
                    "BatchStatement, ResultSet paging and binary values in RegularStatement",
                    e);
        } catch (OperationTimedOutException e) {
            throw new PersistenceException("Timed out",
                    e);

        }
    }


    /*Is this useful?*/
    public static ResultSetFuture checkedAsyncExecute(Session session, Statement statement) throws PersistenceException {
        try {
            return session.executeAsync(statement);
        } catch (NoHostAvailableException e) {
            throw new PersistenceException("no host in the cluster could be contacted successfully to execute this query", e);
        }
    }

}
