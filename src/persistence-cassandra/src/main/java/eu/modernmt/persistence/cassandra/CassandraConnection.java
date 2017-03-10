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
 * Created by davide on 21/09/16.
 */
public class CassandraConnection implements Connection {
    final Session session;

    public CassandraConnection(Cluster cluster, String keyspace) throws PersistenceException {

        try {
            if (keyspace == null) {
                this.session = cluster.connect();
            } else {
                this.session = cluster.connect("\"" + keyspace + "\"");
            }

        } catch (NoHostAvailableException e) {
            throw new PersistenceException("the Cluster has not been initialized yet " +
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

    @Override
    public void close() throws IOException {
        this.session.close();
    }

}
