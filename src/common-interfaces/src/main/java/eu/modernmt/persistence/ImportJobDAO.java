package eu.modernmt.persistence;

import eu.modernmt.model.ImportJob;

/**
 * Created by davide on 21/09/16.
 */
public interface ImportJobDAO {

    ImportJob retrieveById(int id) throws PersistenceException;

    ImportJob put(ImportJob job) throws PersistenceException;

}
