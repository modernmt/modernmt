package eu.modernmt.persistence.sqlite;

import eu.modernmt.model.ImportJob;
import eu.modernmt.persistence.ImportJobDAO;
import eu.modernmt.persistence.PersistenceException;

import java.sql.*;

/**
 * Created by davide on 21/09/16.
 */
public class SQLiteImportJobDAO implements ImportJobDAO {

    private Connection connection;

    public SQLiteImportJobDAO(SQLiteConnection connection) {
        this.connection = connection.connection;
    }

    private static ImportJob read(ResultSet result) throws SQLException {
        long id = result.getLong("id");
        int domain = result.getInt("domain");
        int size = result.getInt("size");
        long begin = result.getLong("begin");
        long end = result.getLong("end");

        ImportJob job = new ImportJob(id, domain);
        job.setSize(size);
        job.setBegin(begin);
        job.setEnd(end);

        return job;
    }

    @Override
    public ImportJob put(ImportJob job) throws PersistenceException {
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = connection.prepareStatement("INSERT INTO import_jobs(\"domain\", \"begin\", \"end\", \"size\") VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, job.getDomain());
            statement.setLong(2, job.getBegin());
            statement.setLong(3, job.getEnd());
            statement.setLong(4, job.getSize());

            statement.executeUpdate();

            result = statement.getGeneratedKeys();

            if (result.next()) {
                job.setId(result.getLong(1));
                return job;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(result);
            SQLUtils.closeQuietly(statement);
        }
    }

    @Override
    public ImportJob retrieveById(int id) throws PersistenceException {
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = connection.prepareStatement("SELECT * FROM import_jobs WHERE \"id\" = ?");
            statement.setInt(1, id);

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
