package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import eu.modernmt.model.ImportJob;
import eu.modernmt.persistence.ImportJobDAO;
import eu.modernmt.persistence.PersistenceException;

import java.util.UUID;

/**
 * Created by andrea on 08/03/17.
 * <p>
 * A CassandraImportJobDAO object offers methods
 * for performing CRUD operations on ImportJob objects
 * when connected to a Cassandra Database.
 */
public class CassandraImportJobDAO implements ImportJobDAO {

    private CassandraConnection connection;

    /**
     * This method creates a CassandraImportJobDao
     * that will communicate with the Cassandra DB under analysis
     * using a specific connection
     *
     * @param connection the Cassandra Connection that the DAO will employ
     *                   to deal with the ImportJob CRUD operations.
     */
    public CassandraImportJobDAO(CassandraConnection connection) {
        this.connection = connection;
    }

    /**
     * This method stores a ImportJob object in the DB
     * with a new, sequentially generated ID
     *
     * @param job the ImportJob object to store in the DB
     * @return the same ImportJob object received as a parameter, updated with its new ID
     * @throws PersistenceException if couldn't insert the importjob in the DB
     */
    @Override
    public ImportJob store(ImportJob job) throws PersistenceException {
        long id = CassandraIdGenerator.generate(connection, CassandraDatabase.IMPORT_JOBS_TABLE_ID);

        String[] columns = {"id", "memory", "\"begin\"", "end", "data_channel", "size"};
        Object[] values = {id, job.getMemory(), job.getBegin(), job.getEnd(), job.getDataChannel(), job.getSize()};
        BuiltStatement statement = QueryBuilder
                .insertInto("import_jobs")
                .values(columns, values)
                .ifNotExists();

        boolean success = CassandraUtils.checkedExecute(connection, statement).wasApplied();

        if (!success)
            throw new PersistenceException("Unable to insert import job into Cassandra Database: " + job);

        job.setId(id);
        return job;
    }

    /**
     * This method receives a unique UUID for an ImportJob,
     * uses it to extract the corresponding ID employed in the DB
     * and retrieves the relative ImportJob from the importjobs table
     *
     * @param uuid the ID of the ImportJob object to retrieve, in the UUID format
     * @return the ImportJob object with the given ID, if there is one in the DB;
     * else, this method returns null
     * @throws PersistenceException
     */
    @Override
    public ImportJob retrieve(UUID uuid) throws PersistenceException {
        long id = ImportJob.getLongId(uuid);

        BuiltStatement statement = QueryBuilder.
                select().
                from("import_jobs").
                where(QueryBuilder.eq("id", id));

        ResultSet result = CassandraUtils.checkedExecute(connection, statement);
        Row row = result.one();

        if (row != null) return read(row);
        else return null;
    }

    /**
     * This method reads a row returned by a query
     * to the importjobs table in the Cassandra DB
     * and creates a new ImportJob object from its fields
     *
     * @param row a row retrieved from the importjobs table
     * @return the new ImportJob object obtained by the row
     */
    private static ImportJob read(Row row) {
        long id = row.getLong("id");
        long memory = row.getLong("memory");
        int size = row.getInt("size");
        long begin = row.getLong("begin");
        long end = row.getLong("end");
        short dataChannel = row.getShort("data_channel");

        ImportJob job = new ImportJob();
        job.setId(id);
        job.setMemory(memory);
        job.setSize(size);
        job.setBegin(begin);
        job.setEnd(end);
        job.setDataChannel(dataChannel);

        return job;
    }
}
