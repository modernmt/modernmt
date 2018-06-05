package eu.modernmt.persistence.mysql;

import eu.modernmt.model.Memory;
import eu.modernmt.persistence.MemoryDAO;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.persistence.mysql.utils.SQLUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by andrea on 29/09/17.
 * A MySQLMemoryDAO object offers methods for performing CRUD operations on Memory objects
 * when connected to a MySQL Database.
 */
public class MySQLMemoryDAO implements MemoryDAO {

    private Connection connection;

    /**
     * Create a MySQLMemoryDAO that will communicate with the MySQLDatabase using a specific connection
     *
     * @param connection the connection to use
     */
    public MySQLMemoryDAO(MySQLConnection connection) {
        this.connection = connection.getDataSourceConnection();
    }


    /**
     * This method retrieves an ImportJob object from the connected MySQL DB using its id.
     *
     * @param id the id of the ImportJob to retrieve, as a UUID object
     * @return the ImportJob stored with the passed id, if there is one; null otherwise
     * @throws PersistenceException if an error occurs
     */
    @Override
    public Memory retrieve(long id) throws PersistenceException {
        String query = "SELECT * FROM mmt_memories WHERE id = ?;";

        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = this.connection.prepareStatement(query);
            statement.setLong(1, id);
            result = statement.executeQuery();

            return result.next() ? read(result) : null;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
            SQLUtils.closeQuietly(result);
        }
    }

    /**
     * This method retrieves from the MySQL DB all the Memories the ids of which are contained in a given collection
     *
     * @param ids the collection of ids of the Memories to retrieve
     * @return a map containing for each passed id the corresponding retrieved Memory
     * @throws PersistenceException if an error occurs
     */
    @Override
    public Map<Long, Memory> retrieve(Collection<Long> ids) throws PersistenceException {
        Map<Long, Memory> memories = new HashMap<>(ids.size());

        /*if the list is empty, return an empty map*/
        if (ids.isEmpty())
            return memories;

        String query = "SELECT * FROM mmt_memories "
                + "WHERE id IN (" + StringUtils.join(ids.toArray(new Long[ids.size()]), ',') + ") ";

        /*execute query and read resources from its result*/
        Statement statement = null;
        ResultSet result = null;
        try {
            statement = this.connection.createStatement();
            result = statement.executeQuery(query);

            while (result.next()) {
                Memory memory = read(result);
                memories.put(memory.getId(), memory);
            }

            return memories;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
            SQLUtils.closeQuietly(result);
        }
    }

    /**
     * This method retrieves from the MySQL DB all the Memory objects
     *
     * @return a list with all the Memory objects in the DB
     * @throws PersistenceException if an error occurs
     */
    @Override
    public Collection<Memory> retrieveAll() throws PersistenceException {
        String query = "SELECT * FROM mmt_memories";

        /*execute query and read resources from its result*/
        Statement statement = null;
        ResultSet result = null;

        try {
            statement = this.connection.createStatement();
            result = statement.executeQuery(query);

            ArrayList<Memory> memories = new ArrayList<>();
            while (result.next())
                memories.add(read(result));
            return memories;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
            SQLUtils.closeQuietly(result);
        }
    }

    /**
     * This method stores a Memory object in the DB
     *
     * @param memory the Memory object to store in the DB
     * @return the Memory itself with the ID it was stored with
     * @throws PersistenceException if could not insert the Memory in the DB
     */
    @Override
    public Memory store(Memory memory) throws PersistenceException {
        return store(memory, false);
    }

    /**
     * This method stores a Memory object in the DB
     *
     * @param memory  the Memory object to store in the DB
     * @param forceId if true, use the ID the memory already has; if false, use an auto incrementing id chosen by the DB
     * @return the stored Memory itself; if forceId was false, it now has the ID it was stored with in the DB
     * @throws PersistenceException if could not insert the Memory in the DB
     */
    @Override
    public Memory store(Memory memory, boolean forceId) throws PersistenceException {
        String query = forceId ? "INSERT INTO mmt_memories (owner, name, id) values (?, ?)" :
                "INSERT INTO mmt_memories (owner, name) values (?)";

        PreparedStatement statement = null;
        ResultSet generatedKeys = null;
        try {
            statement = forceId ? this.connection.prepareStatement(query) :
                    this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            int i = 1;
            statement.setLong(i++, memory.getOwner());
            statement.setString(i++, memory.getName());
            if (forceId)
                statement.setLong(i, memory.getId());

            if (statement.executeUpdate() == 0)
                throw new PersistenceException("Memory store failed, no rows affected.");

            if (!forceId) {
                generatedKeys = statement.getGeneratedKeys();

                if (generatedKeys.next())
                    memory.setId(generatedKeys.getLong(1));
                else
                    throw new PersistenceException("ImportJob store creation failed, no ID obtained.");
            }

            return memory;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
            SQLUtils.closeQuietly(generatedKeys);
        }
    }


    /**
     * This method updates the Memory with the same ID as the passed one, overwriting its name
     * If in the DB there is no row with that ID nothing happens.
     *
     * @param memory the Memory to overwrite
     * @return the passed Memory if the overwrite is successful; null if the memory ID does not correspond to a stored memory.
     * @throws PersistenceException if a DB error occurs
     */
    @Override
    public Memory update(Memory memory) throws PersistenceException {
        String query = "UPDATE TABLE mmt_memories SET name = ? WHERE id = ? ";

        /*execute query and read resources from its result*/
        PreparedStatement statement = null;

        int affectedRows;
        try {
            statement = connection.prepareStatement(query);
            statement.setString(1, memory.getName());
            statement.setLong(2, memory.getId());

            affectedRows = statement.executeUpdate();

            return (affectedRows != 0) ? memory : null;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
        }
    }

    /**
     * This method deletes a Memory object from the DB
     *
     * @param id the id of the Memory object to delete
     * @return True if the object was successfully deleted; false otherwise
     * @throws PersistenceException
     */
    @Override
    public boolean delete(long id) throws PersistenceException {
        String query = "DELETE FROM mmt_memories WHERE id = ? ";
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(query);
            statement.setLong(1, id);
            return (statement.executeUpdate() == 1);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
        }
    }

    private static Memory read(ResultSet result) throws PersistenceException {
        if (result == null)
            return null;

        Memory memory;
        try {
            long id = result.getLong("id");
            long owner = result.getLong("owner");
            String name = result.getString("name");
            memory = new Memory(id, owner, name);
        } catch (SQLException e) {
            throw new PersistenceException(e);
        }
        return memory;
    }

}
