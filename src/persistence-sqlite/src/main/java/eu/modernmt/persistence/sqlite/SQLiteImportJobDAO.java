package eu.modernmt.persistence.sqlite;

import eu.modernmt.model.ImportJob;
import eu.modernmt.persistence.ImportJobDAO;
import eu.modernmt.persistence.PersistenceException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by davide on 21/09/16.
 */
public class SQLiteImportJobDAO implements ImportJobDAO {

    private Connection connection;

    public SQLiteImportJobDAO(SQLiteConnection connection) {
        this.connection = connection.connection;
    }

    private static ImportJob read(ResultSet result) throws SQLException {
        int id = result.getInt("domain");
        long begin = result.getLong("begin");
        long end = result.getLong("end");

        ImportJob job = new ImportJob(id);
        job.setBegin(begin);
        job.setEnd(end);

        return job;
    }

    @Override
    public ImportJob put(ImportJob job) throws PersistenceException {
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement("INSERT INTO import_jobs(\"domain\", \"begin\", \"end\") VALUES (?, ?, ?)");
            statement.setInt(1, job.getDomain());
            statement.setLong(2, job.getBegin());
            statement.setLong(3, job.getEnd());

            return statement.executeUpdate() == 1 ? job : null;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
        }
    }

    @Override
    public ImportJob retrieveByDomainId(int domainId) throws PersistenceException {
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = connection.prepareStatement("SELECT * FROM import_jobs WHERE \"domain\" = ?");
            statement.setInt(1, domainId);

            result = statement.executeQuery();

            return result.next() ? read(result) : null;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(result);
            SQLUtils.closeQuietly(statement);
        }
    }

}
