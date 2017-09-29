package eu.modernmt.cluster.db;

import eu.modernmt.config.DatabaseConfig;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.io.Paths;
import eu.modernmt.model.Memory;
import eu.modernmt.persistence.Connection;
import eu.modernmt.persistence.Database;
import eu.modernmt.persistence.MemoryDAO;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.persistence.cassandra.CassandraDatabase;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;

/**
 * Created by andrea on 31/03/17.
 * This class handles the generation and population of the DB
 * during the execution of MMT start
 * depending on the configuration chosen by the user.
 */
public class DatabaseLoader {

    private final static Logger logger = LogManager.getLogger(DatabaseLoader.class);

    /**
     * This method connects to a Database to interact with,
     * and proceeds to populate it with the baseline memories.
     * <p>
     * The database process can run:
     * - in the same machine (host = localhost):
     * - as an EMBEDDED database process (already launched by the cluster node)
     * - as a pre-existing, STANDALONE database process
     * - in another machine (host != localhost):
     * the db type (embedded or standalone) does not matter to this node
     * <p>
     *
     * @param engine the engine that will employ the DB to load
     * @param config the configuration for the DB to load
     * @return If config enables db usage this method returns
     * the newly instantiated Database object; else, it returns NULL.
     * @throws BootstrapException if createIfMissing is false
     *                            and no DB with the current name was found
     */
    public static Database load(Engine engine, DatabaseConfig config) throws BootstrapException {
        /*create and populate the DB if necessary*/
        logger.info("Connecting to the database...");

        /*if a keyspace name was passed in the config, use it;
        else get a default name in the 1x nomenclature.
        NOTE: 0.15x nomenclature is now discontinued.*/
        String name = config.getName();
        if (name == null)
            name = CassandraDatabase.getDefaultKeyspace();

        // create the Database object (an access point to the db in the running process)
        CassandraDatabase database = new CassandraDatabase(
                config.getHost(),
                config.getPort(),
                name);
        logger.info("Connected to the database");

        // if a db with that name hasn't been created yet in db process,
        Connection connection = null;
        try {
            if (!database.exists()) {
                database.create();

                File baselineMemories = Paths.join(engine.getModelsPath(), "db", "baseline_memories.json");
                List<Memory> memories = BaselineMemoryCollection.load(baselineMemories);
                connection = database.getConnection();

                MemoryDAO memoryDao = database.getMemoryDAO(connection);
                for (Memory memory : memories) {
                    memoryDao.store(memory, true);

                }
                logger.info("Database initialized");
            }
            // if the db is already there, do nothing

        } catch (PersistenceException e) {
            throw new BootstrapException("Unable to initialize the DB", e);
        } finally {
            IOUtils.closeQuietly(connection);
        }
        return database;
    }
}