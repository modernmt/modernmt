package eu.modernmt.persistence;

import eu.modernmt.model.ImportJob;

import java.util.UUID;

/**
 * Created by davide on 21/09/16.
 */
public interface ImportJobDAO {

    ImportJob retrieveById(UUID id) throws PersistenceException;

    ImportJob put(ImportJob job) throws PersistenceException;

}
