package eu.modernmt.persistence;

import eu.modernmt.model.Memory;

import java.util.Collection;
import java.util.Map;

/**
 * Created by davide on 21/09/16.
 */
public interface MemoryDAO {

    Memory retrieve(long id) throws PersistenceException;

    Map<Long, Memory> retrieve(Collection<Long> ids) throws PersistenceException;

    Collection<Memory> retrieveAll() throws PersistenceException;

    Memory store(Memory memory, boolean forceId) throws PersistenceException;

    Memory store(Memory memory) throws PersistenceException;

    Memory update(Memory memory) throws PersistenceException;

    boolean delete(long id) throws PersistenceException;

}
