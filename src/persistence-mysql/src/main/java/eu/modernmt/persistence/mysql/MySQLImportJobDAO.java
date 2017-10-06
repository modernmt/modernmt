package eu.modernmt.persistence.mysql;

import eu.modernmt.model.ImportJob;
import eu.modernmt.persistence.ImportJobDAO;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.persistence.mysql.utils.SQLUtils;

import java.sql.*;
import java.util.UUID;

/**
 * Created by andrea on 29/09/17.
 * A MySQLImportJobDAO object offers methods for performing CRUD operations on ImportJob objects
 * when connected to a MySQL Database.
 */
public class MySQLImportJobDAO implements ImportJobDAO {

    private Connection connection;

    /**
     * Create a MySQLImportJobDAO that will communicate with the MySQLDatabase using a specific connection
     *
     * @param connection the connection to use
     */
    public MySQLImportJobDAO(MySQLConnection connection) {
        this.connection = connection.getDataSourceConnection();
    }


    /**
     * This method retrieves an ImportJob object from the connected MySQL DB using its id.
     *
     * @param id the id of the ImportJob to retrieve, as a UUID object
     * @return the ImportJob stored with the passed id, if there is one; null otherwise
     * @throws PersistenceException
     */
    @Override
    public ImportJob retrieve(UUID id) throws PersistenceException {
        long longId = ImportJob.getLongId(id);

        String query = "SELECT * FROM mmt_import_jobs WHERE id = ?;";

        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = this.connection.prepareStatement(query);
            statement.setLong(1, longId);
            result = statement.executeQuery();
            /*if result.next() is not null return readResource(result), else null*/
            return result.next() ? read(result) : null;

        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
            SQLUtils.closeQuietly(result);
        }
    }


    /**
     * This method stores a ResourceOrigin object in the DB
     *
     * @param importJob the ResourceOrigin object to store in the DB
     * @return if the domain was successfully stored, the method returns
     * the ResourceOrigin itself (with its ID updated to the new one)
     * Else, it throws an exception.
     * @throws PersistenceException if could not insert the ResourceOrigin in the DB
     */
    @Override
    public ImportJob store(ImportJob importJob) throws PersistenceException {
        String query = "INSERT INTO mmt_import_jobs(memory, begin, end, data_channel, size) VALUES (?,?,?,?,?)";

        PreparedStatement statement = null;
        ResultSet generatedKeys = null;
        try {
            statement = this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int i = 1;

            statement.setLong(i++, importJob.getMemory());
            statement.setLong(i++, importJob.getBegin());
            statement.setLong(i++, importJob.getEnd());
            statement.setShort(i++, importJob.getDataChannel());
            statement.setInt(i, importJob.getSize());

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0)
                throw new PersistenceException("ImportJob store failed, no rows affected.");

            generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                importJob.setId(generatedKeys.getLong(1));
                return importJob;
            } else {
                throw new PersistenceException("ImportJob store creation failed, no ID obtained.");
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
            SQLUtils.closeQuietly(generatedKeys);
        }
    }


    /**
     * This method reads the fields of a ResultSet
     * from a table with name "resource_origins"
     * and creates a new ResourceOrigin object
     *
     * @param result a resultSet obtained by a query
     * @return the new ResourceOrigin object obtained by the ResultSet
     * @throws PersistenceException if a required field is not found
     */
    public static ImportJob read(ResultSet result) throws PersistenceException {
        return read(result, "mmt_import_jobs");
    }


    /**
     * This method reads the fields of a ResultSet from the table with given name
     * and creates a new ImportJob object
     *
     * @param result a resultSet obtained by a query
     * @return the new ImportJob object obtained by the ResultSet
     * @throws PersistenceException if a required field is not found
     */
    public static ImportJob read(ResultSet result, String table) throws PersistenceException {
        if (result == null)
            return null;

        ImportJob importJob = new ImportJob();
        try {
            importJob.setId(result.getLong(table + ".id"));
            importJob.setMemory(result.getLong(table + ".memory"));
            importJob.setBegin(result.getLong(table + ".begin"));
            importJob.setEnd(result.getLong(table + ".end"));
            importJob.setDataChannel(result.getShort(table + ".data_channel"));
            importJob.setSize(result.getInt(table + ".size"));
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return importJob;
    }
}
