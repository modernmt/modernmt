package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import eu.modernmt.model.ImportJob;
import eu.modernmt.persistence.ImportJobDAO;
import eu.modernmt.persistence.PersistenceException;

import java.util.UUID;

/**
 * Created by andrea on 08/03/17.
 */

public class CassandraImportJobDAO implements ImportJobDAO {
    private Session session;
    public final int importJobsTableId = 2;

    public CassandraImportJobDAO(CassandraConnection connection) {
        this.session = connection.session;
    }


    @Override
    public ImportJob put(ImportJob job) throws PersistenceException {

        long id = CassandraIdGenerator.generate(session, importJobsTableId);

        String[] columns = {"id", "domain", "\"begin\"", "end", "data_channel", "size"};
        Object[] values = {id, job.getDomain(), job.getBegin(), job.getEnd(), job.getDataChannel(), job.getSize()};
        BuiltStatement statement = QueryBuilder.
                insertInto("import_jobs").
                values(columns, values);

        CassandraUtils.checkedExecute(session, statement);

        job.setId(id);
        return job;
    }

    @Override
    public ImportJob retrieveById(UUID uuid) throws PersistenceException {
        long id = ImportJob.getLongId(uuid);

        BuiltStatement statement = QueryBuilder.
                select().
                from("import_jobs").
                where(QueryBuilder.eq("id", id));

        ResultSet result = CassandraUtils.checkedExecute(session, statement);
        Row row = result.one();

        if (row != null) return read(row);
        else return null;
    }

    private static ImportJob read(Row result) {
        long id = result.getLong("id");
        int domain = result.getInt("domain");
        int size = result.getInt("size");
        long begin = result.getLong("begin");
        long end = result.getLong("end");
        short dataChannel = result.getShort("data_channel");

        ImportJob job = new ImportJob();
        job.setId(id);
        job.setDomain(domain);
        job.setSize(size);
        job.setBegin(begin);
        job.setEnd(end);
        job.setDataChannel(dataChannel);

        return job;
    }
}
