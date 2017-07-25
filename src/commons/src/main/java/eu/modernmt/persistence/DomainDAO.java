package eu.modernmt.persistence;

import eu.modernmt.model.Domain;

import java.util.Collection;
import java.util.Map;

/**
 * Created by davide on 21/09/16.
 */
public interface DomainDAO {

    Domain retrieveById(long id) throws PersistenceException;

    Map<Long, Domain> retrieveByIds(Collection<Long> ids) throws PersistenceException;

    Collection<Domain> retrieveAll() throws PersistenceException;

    Domain put(Domain domain, boolean forceId) throws PersistenceException;

    Domain put(Domain domain) throws PersistenceException;

    Domain update(Domain domain) throws PersistenceException;

    boolean delete(long id) throws PersistenceException;

}
