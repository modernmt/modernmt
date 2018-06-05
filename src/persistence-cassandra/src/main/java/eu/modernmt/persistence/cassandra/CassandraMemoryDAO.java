package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.CodecNotFoundException;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import eu.modernmt.model.Memory;
import eu.modernmt.persistence.MemoryDAO;
import eu.modernmt.persistence.PersistenceException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by andrea on 08/03/17.
 * <p>
 * A CassandraMemoryDAO object offers methods
 * for performing CRUD operations on Memory objects
 * when connected to a Cassandra Database.
 */
public class CassandraMemoryDAO implements MemoryDAO {

    private CassandraConnection connection;

    /**
     * This method creates a CassandraMemoryDAO
     * that will communicate with the Cassandra DB under analysis
     * using a specific connection
     *
     * @param connection the Cassandra Connection that the DAO will employ
     *                   to deal with the Memory CRUD operations.
     */
    public CassandraMemoryDAO(CassandraConnection connection) {
        this.connection = connection;
    }

    /**
     * This method retrieves a Memory object in a Cassandra DB
     * by the ID it was stored with.
     *
     * @param id the ID of the Memory object to retrieve
     * @return the Memory object stored with the passed id, if there is one in the DB;
     * else, this method returns null
     * @throws PersistenceException
     */
    @Override
    public Memory retrieve(long id) throws PersistenceException {
        BuiltStatement statement = QueryBuilder.select()
                .from(CassandraDatabase.MEMORIES_TABLE)
                .where(QueryBuilder.eq("id", id));

        ResultSet result = CassandraUtils.checkedExecute(connection, statement);
        return read(result.one());
    }

    /**
     * This method reads a row returned by a query
     * to the memories table in the Cassandra DB
     * and creates a new Memory object from its fields
     *
     * @param row a row retrieved from the memories table
     * @return the new Memory object obtained by the row
     * @throws PersistenceException
     */
    private static Memory read(Row row) throws PersistenceException {
        if (row == null)
            return null;

        try {
            long id = row.getLong("id");
            String name = row.getString("name");
            long owner = row.getLong("owner");

            return new Memory(id, owner, name);
        } catch (IllegalArgumentException e) {
            throw new PersistenceException("code name not valid for this object", e);
        } catch (CodecNotFoundException e) {
            throw new PersistenceException("there is no registered codec to convert the underlying CQL type to a long", e);
        }
    }

    /**
     * This method retrieves from the Cassandra DB
     * all the Memory objects the ids of which
     * are contained in a given collection
     *
     * @param ids the collection of ids of the Memories to retrieve
     * @return the Memory objects the ids of which are contained in the passed id collection
     * @throws PersistenceException
     */
    @Override
    public Map<Long, Memory> retrieve(Collection<Long> ids) throws PersistenceException {
        Map<Long, Memory> map = new HashMap<>(ids.size());

        /*if the list is empty, return an empty map*/
        if (ids.isEmpty())
            return map;

        ArrayList<Long> list = new ArrayList<>(ids.size());
        list.addAll(ids);
        BuiltStatement statement = QueryBuilder.
                select().
                from(CassandraDatabase.MEMORIES_TABLE).
                where(QueryBuilder.in("id", list));

        /*execute the query*/
        ResultSet result = CassandraUtils.checkedExecute(connection, statement);

        /*create the Memory objects from the rows*/
        while (!result.isExhausted()) {
            Memory memory = read(result.one());
            map.put(memory.getId(), memory);
        }

        return map;
    }

    /**
     * This method retrieves from the Cassandra DB
     * all the Memory objects stored in the corresponding table
     *
     * @return a list with all the Memory objects in the DB
     * @throws PersistenceException
     */
    @Override
    public Collection<Memory> retrieveAll() throws PersistenceException {
        ArrayList<Memory> list = new ArrayList<>();

        BuiltStatement statement = QueryBuilder.select().
                from(CassandraDatabase.MEMORIES_TABLE).
                where();
        ResultSet result = CassandraUtils.checkedExecute(connection, statement);

        for (Row row : result.all())
            list.add(read(row));

        return list;
    }

    /**
     * This method stores a Memory object in the DB
     * with a new, sequentially generated ID
     *
     * @param memory the Memory object to store in the DB
     * @return the same Memory object received as a parameter, updated with its new ID
     * @throws PersistenceException if couldn't insert the importjob in the DB
     */
    @Override
    public Memory store(Memory memory) throws PersistenceException {
        return store(memory, false);
    }

    /**
     * This method stores a Memory object in the DB
     *
     * @param memory  the Memory object to store in the DB
     * @param forceId if it is true, then this method tries to store memory with its ID.
     *                Else it uses a new, sequentially generated ID.
     * @return if the memory was successfully stored, the method returns memory itself
     * (with its ID update to the new one if forceId was false).
     * Else, throws an exception.
     * @throws PersistenceException if couldn't insert the importjob in the DB
     */
    @Override
    public Memory store(Memory memory, boolean forceId) throws PersistenceException {
        long id;

        if (!forceId) {
            id = CassandraIdGenerator.generate(connection, CassandraDatabase.MEMORIES_TABLE_ID);
        } else {
            id = memory.getId();
            CassandraIdGenerator.advanceCounter(connection, CassandraDatabase.MEMORIES_TABLE_ID, id);
        }

        String[] columns = {"id", "owner", "name"};
        Object[] values = {id, memory.getOwner(), memory.getName()};

        BuiltStatement statement = QueryBuilder
                .insertInto(CassandraDatabase.MEMORIES_TABLE)
                .values(columns, values)
                .ifNotExists();

        boolean success = CassandraUtils.checkedExecute(connection, statement).wasApplied();

        if (!success)
            throw new PersistenceException("Unable to insert memory into Cassandra Database: " + memory);

        memory.setId(id);

        return memory;
    }

    /**
     * This method receives a Memory object
     * and stores it in the DB overwriting an existing row with same ID.
     * If in the DB there is no row with that ID nothing happens.
     *
     * @param memory the Memory object to store in the DB
     *               in place of an already existing one
     * @return the same memory object passed as a parameter,
     * if the overwrite is successful
     * (if an object with that ID was already in the DB)
     * or null if the overwrite was not successful.
     * @throws PersistenceException
     */
    @Override
    public Memory update(Memory memory) throws PersistenceException {
        BuiltStatement built = QueryBuilder.update(CassandraDatabase.MEMORIES_TABLE)
                .with(QueryBuilder.set("name", memory.getName()))
                .where(QueryBuilder.eq("id", memory.getId()))
                .ifExists();

        ResultSet result = CassandraUtils.checkedExecute(connection, built);

        if (result.wasApplied())
            return memory;
        else
            return null;
    }

    /**
     * This method deletes a Memory object from the DB
     *
     * @param id the id of the Memory object to delete
     * @return True if the object was successfully deleted;
     * False if no object with the passed ID could be found.
     * @throws PersistenceException
     */
    @Override
    public boolean delete(long id) throws PersistenceException {
        BuiltStatement built = QueryBuilder.delete().
                from(CassandraDatabase.MEMORIES_TABLE).
                where(QueryBuilder.eq("id", id)).
                ifExists();

        ResultSet result = CassandraUtils.checkedExecute(connection, built);

        return result.wasApplied();
    }
}