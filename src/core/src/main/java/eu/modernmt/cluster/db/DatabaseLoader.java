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
     * with the running database process (launched by Python).
     * The database process can run:
     * - in the same machine (type = EMBEDDED, Leader node);
     * - in another machine also hosting one or more engines
     * (Type = Embedded, Follower node);
     * - in another machine only dedicated to the DB
     * (type =  STANDALONE).
     * <p>
     * This method then proceeds to populate the database if necessary.
     * <p>
     * Otherwise, if the configuration disables the database usage,
     * this method does nothing
     *
     * @param engine the engine that will employ the DB to load
     * @param config the configuration for the DB to load
     * @return If config enables db usage this method returns
     * the newly instantiated Database object; else, it returns NULL.
     */
    public static Database load(Engine engine, DatabaseConfig config, boolean createIfMissing) throws BootstrapException {

        Database database = null;

        /* create and populate the DB
        only if the configuration file does not disable it
        (else, its process hasn't even been started by Python)*/
        if (config.isEnabled()) {
            logger.info("Starting Database");
            // if the database is embeddedd, use the 0.15x version name convention;
            // else, use the 1x name convention

            String name = config.getName();

            /*if a keyspace name was not passed, then figure a name
             * with the default nomenclatures, depending
             * whether the type is EMBEDDED or not;
             * otherwise, use the passed name*/
            if (name == null) {
                if (config.getType() == DatabaseConfig.Type.EMBEDDED) {
                    name = CassandraDatabase.getDefaultKeyspace();
                } else {
                    name = CassandraDatabase.getDefaultKeyspace(engine.getName(), engine.getSourceLanguage(), engine.getTargetLanguage());
                }
            }

            // create the Database object:
            // it's an access point to the db in the running db process
            database = new CassandraDatabase(
                    config.getHost(),
                    config.getPort(),
                    name);
            logger.info("Database started");

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
                        // else, throw an exception
                    } else {
                        throw new BootstrapException("Missing database: " + name);
                    }

                }
                // if the db is already there, do nothing:
                // it obviously does not need another initialization
            } catch (PersistenceException e) {
                throw new BootstrapException("Unable to initialize the DB");
            } finally {
                IOUtils.closeQuietly(connection);
            }
        }
        /*database has been updated only if the configuration enabled it;
        * else, it is still null*/
        return database;
    }
}