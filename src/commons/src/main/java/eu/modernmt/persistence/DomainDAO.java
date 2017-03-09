package eu.modernmt.persistence;

import eu.modernmt.model.Domain;

import java.util.Collection;
import java.util.Map;

/**
 * Created by davide on 21/09/16.
 */
public interface DomainDAO {

    Domain retrieveById(int id) throws PersistenceException;

    Map<Integer, Domain> retrieveByIds(Collection<Integer> ids) throws PersistenceException;

    Collection<Domain> retrieveAll() throws PersistenceException;

    Domain put(Domain domain) throws PersistenceException;

    Domain update(Domain domain) throws PersistenceException;

    boolean delete(int id) throws PersistenceException;

}
