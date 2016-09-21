package eu.modernmt.persistence;

import eu.modernmt.model.Domain;

import java.util.Collection;

/**
 * Created by davide on 21/09/16.
 */
public interface DomainDAO {

    Domain retrieveBytId(int id) throws PersistenceException;

    Collection<Domain> retrieveAll() throws PersistenceException;

    Domain put(Domain domain) throws PersistenceException;

    Domain update(Domain domain) throws PersistenceException;

}
