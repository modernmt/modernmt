package eu.modernmt.persistence;

import eu.modernmt.model.ImportJob;

import java.util.UUID;

/**
 * Created by davide on 21/09/16.
 */
public interface ImportJobDAO {

    ImportJob retrieve(UUID id) throws PersistenceException;

    ImportJob store(ImportJob job) throws PersistenceException;

}
