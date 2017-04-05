package eu.modernmt.cluster.db;

import eu.modernmt.config.DatabaseConfig;
import eu.modernmt.engine.BootstrapException;
import eu.modernmt.engine.Engine;
import eu.modernmt.io.Paths;
import eu.modernmt.model.Domain;
import eu.modernmt.persistence.Connection;
import eu.modernmt.persistence.Database;
import eu.modernmt.persistence.DomainDAO;
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
 * during the execution of MMT start (et similia),
 * depending on the configuration chosen by the user.
 */
public class DatabaseLoader {

    private final static Logger logger = LogManager.getLogger(DatabaseLoader.class);

    /**
     * If the configuration enables the database usage,
     * this method generates a Database object to interact
     * with the running database process.
     * The database process can run:
     * - in the same machine (type = EMBEDDED, Leader node);
     * - in another machine in the same MMT cluster
     * (Type = EMBEDDED, Follower node);
     * - in a separate, remote machine not belonging to the cluster:
     * (type = STANDALONE).
     * <p>
     * This method then proceeds to populate the database if necessary.
     * <p>
     * Otherwise, if the configuration disables the database usage,
     * this method does nothing
     *
     * @param engine          the engine that will employ the DB to load
     * @param config          the configuration for the DB to load
     * @param createIfMissing if true, if in the current db process there is no database
     *                        with the name we seek, create it from scratch.
     *                        If false, if in the current db process there is no database
     *                        with the name we seek, raise an exception.
     * @return If config enables db usage this method returns
     * the newly instantiated Database object; else, it returns NULL.
     * @throws BootstrapException if createIfMissing is false
     *                            and no DB with the current name was found
     */
    public static Database load(Engine engine, DatabaseConfig config, boolean createIfMissing) throws BootstrapException {

        Database database = null;

        /*create and populate the DB if necessary*/

        /*if the DB usage is enabled*/
        if (config.isEnabled()) {
            logger.info("Connecting to the database...");

            /*if a keyspace name was passed in the config, use it; else get a default name;
            employ either the 0.15x name convention if type is embedded
            or the 1x name convention otherwise*/
            String name = config.getName();
            if (name == null) {
                if (config.getType() == DatabaseConfig.Type.EMBEDDED) {
                    name = CassandraDatabase.getDefaultKeyspace();
                } else {
                    name = CassandraDatabase.getDefaultKeyspace(engine.getName(), engine.getSourceLanguage(), engine.getTargetLanguage());
                }
            }

            // create the Database object (an access point to the db in the running process)
            database = new CassandraDatabase(
                    config.getHost(),
                    config.getPort(),
                    name);
            logger.info("Connected to the database");

            // if a db with that name hasn't been created yet in db process,
            Connection connection = null;
            try {
                if (!database.exists()) {
                    // if the db should create a db when it doesn't exist yet,
                    // then create it and populate it
                    if (createIfMissing) {
                        database.create();
                        File baselineDomains = Paths.join(engine.getModelsPath(), "db", "baseline_domains.json");
                        List<Domain> domains = BaselineDomainsCollection.load(baselineDomains);
                        connection = database.getConnection();
                        DomainDAO domainDao = database.getDomainDAO(connection);
                        for (Domain domain : domains) {
                            domainDao.put(domain, true);
                        }
                        logger.info("Database initialized");
                    }
                    // else, throw an exception
                    else {
                        throw new BootstrapException("Missing database: " + name);
                    }
                }
                // if the db is already there, do nothing

            } catch (PersistenceException e) {
                throw new BootstrapException("Unable to initialize the DB");
            } finally {
                IOUtils.closeQuietly(connection);
            }
        }

        /*the db has been updated if the configuration let it; else it is still null*/
        return database;
    }
}