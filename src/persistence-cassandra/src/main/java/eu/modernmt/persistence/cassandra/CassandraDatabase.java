package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.schemabuilder.DropKeyspace;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import eu.modernmt.persistence.*;
import org.apache.commons.io.IOUtils;

/**
 * Created by andrearossi on 08/03/17.
 * A CassandraDatabase object represents the access point
 * to a Cassandra DB instance.
 * It offers methods for establishing connections with the DB
 * and to get DAO objects for the entities it stores.
 */
public class CassandraDatabase extends Database {

    public static final String MEMORIES_TABLE = "memories";
    public static final String IMPORT_JOBS_TABLE = "import_jobs";
    public static final String COUNTERS_TABLE = "table_counters";
    public static final String INITIALIZATION_METADATA = "metadata";

    public static final int MEMORIES_TABLE_ID = 1;
    public static final int IMPORT_JOBS_TABLE_ID = 2;
    public static final int[] TABLE_IDS = {MEMORIES_TABLE_ID, IMPORT_JOBS_TABLE_ID};

    private final String keyspace;
    private final String host;
    private final int port;

    private Cluster cluster;

    /**
     * This method returns the default keyspace name
     *
     * @return the keyspace name
     */
    public static String getDefaultKeyspace() {
        return "default";
    }

    /**
     * This constructor builds an access point to a Cassandra DB
     * and, in particular, to one of its keyspaces
     *
     * @param host     the hostname of the machine that is running Cassandra
     * @param port     the port on which the Cassandra machine is listening to
     * @param keyspace the keyspace in which the target entities are stored in the Cassandra DB
     */
    public CassandraDatabase(String host, int port, String keyspace) {
        if (keyspace == null)
            throw new NullPointerException("keyspace");
        this.keyspace = keyspace;
        this.host = host;
        this.port = port;

        initCluster();
    }

    /**
     * This method creates an access point to the database to work with.
     * If the current cluster objec is already initialized,
     * the method closes it and rebuilds it from scratch.
     */
    private void initCluster() {
        if (this.cluster != null)
            this.cluster.close();
        this.cluster = Cluster.builder().withPort(port).addContactPoint(host).build();
    }

    /**
     * This method provides a connection to a Cassandra DB
     *
     * @param cached
     * @return A CassandraConnection object, that
     * can be used to establish a communication Session with the DB
     * @throws PersistenceException
     */
    @Override
    public CassandraConnection getConnection(boolean cached) throws PersistenceException {
        return new CassandraConnection(this.cluster, this.keyspace);
    }

    /**
     * This method creates and returns a DAO for Memory objects
     *
     * @param connection a currently active connection to the DB
     * @return A MemoryDAO that can perform CRUD operations for Memory objects
     */
    @Override
    public MemoryDAO getMemoryDAO(Connection connection) {
        return new CassandraMemoryDAO((CassandraConnection) connection);
    }

    /**
     * This method creates and returns a DAO for ImportJob objects
     *
     * @param connection a currently active connection to the DB
     * @return An ImportJobDao that can perform CRUD operations for ImportJob objects
     */
    @Override
    public ImportJobDAO getImportJobDAO(Connection connection) {
        return new CassandraImportJobDAO((CassandraConnection) connection);
    }

    /**
     * This method establishes a new connection to the DB
     * and drops the current keyspace (with all its tables).
     * If the keyspace has already been dropped, the method does nothing.
     *
     * @throws PersistenceException
     */
    public void drop() throws PersistenceException {
        CassandraConnection connection = null;

        try {
            connection = new CassandraConnection(this.cluster, null);
            DropKeyspace dropKeyspace = SchemaBuilder.dropKeyspace('"' + this.keyspace + '"').ifExists();
            CassandraUtils.checkedExecute(connection, dropKeyspace);

        } catch (KeyspaceNotFoundException e) {
            /*ignore*/
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    /**
     * This method establishes a new connection to the DB
     * and uses it to create
     * - a new keyspace
     * - a new memories table
     * - a new importjobs table
     * - a new table_counters table,
     * - a new initialization metadata table,
     * with an entry for memory and another one for importjobs
     * and with an entry in the initialization metadata table
     *
     * @throws PersistenceException
     */
    @Override
    public void create() throws PersistenceException {
        CassandraConnection connection = null;

        try {
            connection = new CassandraConnection(this.cluster, null);

            String createKeyspace =
                    "CREATE KEYSPACE \"" + this.keyspace + "\" WITH replication = " +
                            "{'class':'SimpleStrategy', 'replication_factor':1};";

            ResultSet result = CassandraUtils.checkedExecute(connection, createKeyspace);
            if (!result.getExecutionInfo().isSchemaInAgreement())
                throw new PersistenceException("Cassandra schema agreement time out: CREATE KEYSPACE");
        } finally {
            IOUtils.closeQuietly(connection);
        }

        connection = null;
        try {
            connection = new CassandraConnection(this.cluster, this.keyspace);

            SimpleStatement createCountersTable = new SimpleStatement(
                    "CREATE TABLE IF NOT EXISTS " + COUNTERS_TABLE +
                            " (table_id int PRIMARY KEY, table_counter bigint);");

            SimpleStatement createMemoriesTable = new SimpleStatement(
                    "CREATE TABLE IF NOT EXISTS " + MEMORIES_TABLE +
                            " (id bigint PRIMARY KEY, owner bigint, name varchar);");

            SimpleStatement createImportJobsTable = new SimpleStatement(
                    "CREATE TABLE IF NOT EXISTS " + IMPORT_JOBS_TABLE +
                            " (id bigint PRIMARY KEY, memory bigint, size int, \"begin\" bigint, end bigint, data_channel smallint);");

            SimpleStatement createInitializationTable = new SimpleStatement(
                    "CREATE TABLE IF NOT EXISTS " + INITIALIZATION_METADATA +
                            " (id bigint PRIMARY KEY, initialized Boolean);");


            CassandraUtils.checkedExecute(connection, createCountersTable);
            CassandraUtils.checkedExecute(connection, createMemoriesTable);
            CassandraUtils.checkedExecute(connection, createImportJobsTable);
            CassandraUtils.checkedExecute(connection, createInitializationTable);
            this.populateInitializationTable();
            CassandraIdGenerator.initializeTableCounter(connection, TABLE_IDS);
        } finally {
            IOUtils.closeQuietly(connection);
        }

        /*It is necessary to close and restart the cluster object because
         * we need Cassandra to refresh its internal structures.
         * Otherwise internal queries might result in random results.
         * (Issue with counters_table not updated with the correct table_counters: values always at 0)*/
        initCluster();
    }

    /**
     * This method states if the current keyspace exists or not
     *
     * @return True if the current keyspace exists in the DB; else, false
     */
    @Override
    public boolean exists() {
        return getKeyspaceMetadata() != null;
    }

    @Override
    public String getName() {
        return this.keyspace;
    }


    /**
     * This method tries to set to true the initialized field in the initialization metadata.
     * If the field was already true, meaning that the DB was already initialized, this method will return false.
     *
     * @return true if could update the initialization metadata, false otherwise.
     * @throws PersistenceException if a DB error occurs
     */
    @Override
    public boolean initialize() throws PersistenceException {
        CassandraConnection connection = null;
        try {
            connection = (CassandraConnection) this.getConnection();
            BuiltStatement built = QueryBuilder.update(INITIALIZATION_METADATA).
                    with(QueryBuilder.set("initialized", true)).
                    where(QueryBuilder.eq("id", 1)).
                    onlyIf(QueryBuilder.eq("initialized", false));
            ResultSet result = CassandraUtils.checkedExecute(connection, built);
            return result.wasApplied();
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    @Override
    public void close() {
        this.cluster.close();
    }

    private KeyspaceMetadata getKeyspaceMetadata() {
        return this.cluster.getMetadata().getKeyspace('"' + this.keyspace + '"');
    }

    @Override
    public void testConnection() throws PersistenceException {
        CassandraConnection connection = null;

        try {
            connection = getConnection(true);
            if (connection.session.isClosed())
                throw new PersistenceException("connection closed");
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    /**
     * This method inserts an entry id:1, initialized:false in the initialization metadata.
     * If there is already an entry with that id, this method will do nothing.
     *
     * @throws PersistenceException if a DB error occurs
     */
    private void populateInitializationTable() throws PersistenceException {
        CassandraConnection connection = null;

        try {
            connection = (CassandraConnection) this.getConnection();
            String[] columns = {"id", "initialized"};
            Object[] values = {1, false};
            BuiltStatement built = QueryBuilder
                    .insertInto(INITIALIZATION_METADATA)
                    .values(columns, values)
                    .ifNotExists();
            CassandraUtils.checkedExecute(connection, built);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

}