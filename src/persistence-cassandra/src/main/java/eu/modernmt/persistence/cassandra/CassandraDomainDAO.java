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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by andrea on 08/03/17.
 */
public class CassandraDomainDAO implements DomainDAO {
    private Session session;
    public final int domainsTableId = 1;

    public CassandraDomainDAO(CassandraConnection connection) {
        this.session = connection.session;
    }


    @Override
    public Domain retrieveById(int id) throws PersistenceException {

        BuiltStatement statement = QueryBuilder.select()
                .from("domains")
                .where(QueryBuilder.eq("id", id));

        ResultSet result = CassandraUtils.checkedExecute(session, statement);
        return read(result.one());
    }

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
                from("domains").
                where(QueryBuilder.in("id", list));

        /*execute the query*/
        ResultSet result = CassandraUtils.checkedExecute(session, statement);

        /*create the Domain objects from the rows*/
        while (!result.isExhausted()) {
            Domain domain = read(result.one());
            map.put(domain.getId(), domain);
        }

        return map;
    }

    @Override
    public Collection<Domain> retrieveAll() throws PersistenceException {
        ArrayList<Domain> list = new ArrayList<>();

        BuiltStatement statement = QueryBuilder.select().
                from("domains").
                where();
        ResultSet result = CassandraUtils.checkedExecute(session, statement);

        for (Row row : result.all()) {
            list.add(read(row));
        }
        return list;
    }

    @Override
    public Domain put(Domain domain) throws PersistenceException {
        int id = (int) CassandraIdGenerator.generate(session, domainsTableId);
        String[] columns = {"id", "name"};
        Object[] values = {id, domain.getName()};

        BuiltStatement statement = QueryBuilder.insertInto("domains").
                values(columns, values);

        CassandraUtils.checkedExecute(session, statement);
        domain.setId(id);
        return domain;
    }

    @Override
    public Domain update(Domain domain) throws PersistenceException {

        BuiltStatement built = QueryBuilder.update("domains").
                with(QueryBuilder.set("name", domain.getName())).
                where(QueryBuilder.eq("id", domain.getId())).
                ifExists();

        ResultSet result = CassandraUtils.checkedExecute(session, built);

        if (result.wasApplied())
            return domain;
        else
            return null;
    }

    @Override
    public boolean delete(int id) throws PersistenceException {

        BuiltStatement built = QueryBuilder.delete().
                from("domains").
                where(QueryBuilder.eq("id", id)).
                ifExists();

        ResultSet result = CassandraUtils.checkedExecute(session, built);

        return result.wasApplied();
    }
}