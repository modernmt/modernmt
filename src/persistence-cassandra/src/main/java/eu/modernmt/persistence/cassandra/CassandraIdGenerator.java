package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import eu.modernmt.persistence.PersistenceException;

/**
 * Created by andrea on 08/03/17.
 */
public class CassandraIdGenerator {

    public static long generate(Session session, int tableId) throws PersistenceException {

        String keyspace = session.getLoggedKeyspace();
        if (keyspace == null || keyspace.equals("default")) {
            keyspace = "\"default\"";
        }

        BuiltStatement get = QueryBuilder.select("table_counter").
                from(keyspace, CassandraDatabase.COUNTERS_TABLE).
                where(QueryBuilder.eq("table_id", tableId));

        while (true) {
            long oldCount = CassandraUtils.checkedExecute(session, get).one().getLong("table_counter");

            //String set = "UPDATE " + keyspace + "." + CassandraDatabase.COUNTERS_TABLE + " SET table_counter = " + (oldCount + 1L) + " WHERE table_id = " + tableId + " IF table_counter = " + oldCount + ";";
            BuiltStatement set = QueryBuilder.update(keyspace, CassandraDatabase.COUNTERS_TABLE).
                    with(QueryBuilder.set("table_counter", (oldCount + 1L))).
                    where(QueryBuilder.eq("table_id", tableId)).
                    onlyIf(QueryBuilder.eq("table_counter", oldCount));

            if (CassandraUtils.checkedExecute(session, set).wasApplied())
                return oldCount + 1L;
        }
    }
}