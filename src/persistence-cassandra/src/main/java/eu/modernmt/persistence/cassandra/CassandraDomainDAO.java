package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.CodecNotFoundException;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import eu.modernmt.model.Domain;
import eu.modernmt.persistence.DomainDAO;
import eu.modernmt.persistence.PersistenceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by andrea on 08/03/17.
 * <p>
 * A CassandraDomainDao object offers methods
 * for performing CRUD operations on Domain objects
 * when connected to a Cassandra Database.
 */
public class CassandraDomainDAO implements DomainDAO {
    private CassandraConnection connection;

    /**
     * This method creates a CassandraDomainDao
     * that will communicate with the Cassandra DB under analysis
     * using a specific connection
     *
     * @param connection the Cassandra Connection that the DAO will employ
     *                   to deal with the Domain CRUD operations.
     */
    public CassandraDomainDAO(CassandraConnection connection) {
        this.connection = connection;
    }


    /**
     * This method retrieves a Domain object in a Cassandra DB
     * by the ID it was stored with.
     *
     * @param id the ID of the Domain object to retrieve
     * @return the Domain object stored with the passed id, if there is one in the DB;
     * else, this method returns null
     * @throws PersistenceException
     */
    @Override
    public Domain retrieveById(int id) throws PersistenceException {

        BuiltStatement statement = QueryBuilder.select()
                .from(CassandraDatabase.DOMAINS_TABLE)
                .where(QueryBuilder.eq("id", id));

        ResultSet result = CassandraUtils.checkedExecute(connection, statement);
        return read(result.one());
    }

    /**
     * This method reads a row returned by a query
     * to the domains table in the Cassandra DB
     * and creates a new Domain object from its fields
     *
     * @param row a row retrieved from the domains table
     * @return the new Domain object obtained by the row
     * @throws PersistenceException
     */
    private static Domain read(Row row) throws PersistenceException {

        if (row == null) return null;
        try {
            int id = row.getInt("id");
            String name = row.getString("name");
            return new Domain(id, name);

        } catch (IllegalArgumentException e) {
            throw new PersistenceException("code name not valid for this object", e);
        } catch (CodecNotFoundException e) {
            throw new PersistenceException("there is no registered codec to convert the underlying CQL type to a long", e);
        }
    }

    /**
     * This method retrieves from the Cassandra DB
     * all the Domain objects the ids of which
     * are contained in a given collection
     *
     * @param ids the collection of ids of the Domains to retrieve
     * @return the Domain objects the ids of which are contained in the passed id collection
     * @throws PersistenceException
     */
    @Override
    public Map<Integer, Domain> retrieveByIds(Collection<Integer> ids) throws PersistenceException {

        /*result map*/
        Map<Integer, Domain> map = new HashMap<>(ids.size());

        /*if the list is empty, return an empty map*/
        if (ids.isEmpty())
            return map;

        ArrayList<Integer> list = new ArrayList<>(ids.size());
        list.addAll(ids);
        BuiltStatement statement = QueryBuilder.
                select().
                from(CassandraDatabase.DOMAINS_TABLE).
                where(QueryBuilder.in("id", list));

        /*execute the query*/
        ResultSet result = CassandraUtils.checkedExecute(connection, statement);

        /*create the Domain objects from the rows*/
        while (!result.isExhausted()) {
            Domain domain = read(result.one());
            map.put(domain.getId(), domain);
        }

        return map;
    }

    /**
     * This method retrieves from the Cassandra DB
     * all the Domain objects stored in the corresponding table
     *
     * @return a list with all the Domain objects in the DB
     * @throws PersistenceException
     */
    @Override
    public Collection<Domain> retrieveAll() throws PersistenceException {
        ArrayList<Domain> list = new ArrayList<>();

        BuiltStatement statement = QueryBuilder.select().
                from(CassandraDatabase.DOMAINS_TABLE).
                where();
        ResultSet result = CassandraUtils.checkedExecute(connection, statement);

        for (Row row : result.all()) {
            list.add(read(row));
        }
        return list;
    }

    /**
     * This method stores a Domain object in the DB
     * with a new, sequentially generated ID
     *
     * @param domain the Domain object to store in the DB
     * @return the same Domain object received as a parameter, updated with its new ID
     * @throws PersistenceException
     */
    @Override
    public Domain put(Domain domain) throws PersistenceException {
        return this.put(domain, false);
    }

    /**
     * This method stores a Domain object in the DB
     *
     * @param domain  the Domain object to store in the DB
     * @param forceId if it is true, then this method tries to store domain with its ID.
     *                Else it uses a new, sequentially generated ID.
     * @return if the domain was successfully stored, the method returns domain itself
     * (with its ID update to the new one if forceId was false).
     * Else, throws an exception.
     * @throws PersistenceException
     */
    @Override
    public Domain put(Domain domain, boolean forceId) throws PersistenceException {
        int id;

        if (!forceId) {
            id = (int) CassandraIdGenerator.generate(connection, CassandraDatabase.DOMAINS_TABLE_ID);
        } else {
            id = domain.getId();
            CassandraIdGenerator.advanceCounter(connection, CassandraDatabase.DOMAINS_TABLE_ID, id);
        }

        String[] columns = {"id", "name"};
        Object[] values = {id, domain.getName()};

        BuiltStatement statement = QueryBuilder.insertInto(CassandraDatabase.DOMAINS_TABLE).
                values(columns, values);

        CassandraUtils.checkedExecute(connection, statement);
        domain.setId(id);
        return domain;
    }

    /**
     * This method receives a Domain object
     * and stores it in the DB overwriting an existing row with same ID.
     * If in the DB there is no row with that ID nothing happens.
     *
     * @param domain the Domain object to put in the DB
     *               in place of an already existing one
     * @return the same domain object passed as a parameter,
     * if the overwrite is successful
     * (if an object with that ID was already in the DB)
     * or null if the overwrite was not successful.
     * @throws PersistenceException
     */
    @Override
    public Domain update(Domain domain) throws PersistenceException {

        BuiltStatement built = QueryBuilder.update(CassandraDatabase.DOMAINS_TABLE).
                with(QueryBuilder.set("name", domain.getName())).
                where(QueryBuilder.eq("id", domain.getId())).
                ifExists();

        ResultSet result = CassandraUtils.checkedExecute(connection, built);

        if (result.wasApplied())
            return domain;
        else
            return null;
    }

    /**
     * This method deletes a Domain object from the DB
     *
     * @param id the id of the Domain object to delete
     * @return True if the object was successfully deleted;
     * False if no object with the passed ID could be found.
     * @throws PersistenceException
     */
    @Override
    public boolean delete(int id) throws PersistenceException {

        BuiltStatement built = QueryBuilder.delete().
                from(CassandraDatabase.DOMAINS_TABLE).
                where(QueryBuilder.eq("id", id)).
                ifExists();

        ResultSet result = CassandraUtils.checkedExecute(connection, built);

        return result.wasApplied();
    }
}