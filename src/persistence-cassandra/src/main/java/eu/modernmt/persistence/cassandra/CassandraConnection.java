package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AuthenticationException;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import eu.modernmt.persistence.Connection;
import eu.modernmt.persistence.PersistenceException;

import java.io.IOException;

/**
 * Created by andrea on 09/03/17.
 * A CassandraConnection object represents a connection
 * with a Cassandra DataBase.
 */
public class CassandraConnection implements Connection {
    final Session session;


    /**
     * This constructor builds a CassandraConnection object
     * to communicate with a specific DB
     * and, possibly, with a specific keyspace inside it.
     *
     * @param cluster  An object that represents an access point
     *                 for the DB to connect to.
     *                 It is identified by its hostname and port.
     * @param keyspace The Cassandra keyspace to interact with.
     *                 If is allowed to be null too.
     */
    public CassandraConnection(Cluster cluster, String keyspace) throws PersistenceException {

        try {
            if (keyspace == null) {
                this.session = cluster.connect();
            } else {
                this.session = cluster.connect(keyspace);
            }

        } catch (NoHostAvailableException e) {
            throw new PersistenceException("the Cassandra cluster has not been initialized yet " +
                    " and no host amongst the contact points can be reached, " +
                    " or no host can be contacted to set the keyspace", e);
        } catch (AuthenticationException e) {
            throw new PersistenceException("An authentication error occurred while " +
                    " contacting the initial contact points", e);
        } catch (InvalidQueryException e) {
            throw new KeyspaceNotFoundException("The keyspace does not exist", e);
        } catch (IllegalStateException e) {
            throw new PersistenceException("Cluster was closed prior to calling this method." +
                    " This can occur either directly or as a result" +
                    " of an error while initializing the Cluster", e);
        }
    }

    /**
     * This method closes the session with the current DB
     */
    @Override
    public void close() throws IOException {
        this.session.close();
    }

}
