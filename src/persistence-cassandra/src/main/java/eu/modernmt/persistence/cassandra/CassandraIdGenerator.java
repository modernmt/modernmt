package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.BuiltStatement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import eu.modernmt.persistence.PersistenceException;

/**
 * This class provides static methods
 * that generate sequential integer IDs
 * for objects to store in our Cassandra DB.
 */
public class CassandraIdGenerator {

    /**
     * This method generates a new ID for a new object
     * that must be stored in a certain table.
     * The new IDs are long and are generated in a sequential way.
     * <p>
     * This method is thread-safe.
     *
     * @param session the current session in the active CassandraConnection
     * @param tableId the ID of the table in which we want to store a new
     * @return the newly generated ID,
     * @throws PersistenceException
     */
    public static long generate(Session session, int tableId) throws PersistenceException {

        String keyspace = session.getLoggedKeyspace();
        if (keyspace == null || keyspace.equals("default")) {
            keyspace = "\"default\"";
        }

        /*the table COUNTERS_TABLE has a row for each other table in our db;
        each row holds the table id and a counter marking the last ID
        that has been employed when storing an object in that table.*/

        /*statement for getting the last ID used in the table under analysis
        * from the Counters_table*/
        BuiltStatement get = QueryBuilder.select("table_counter").
                from(keyspace, CassandraDatabase.COUNTERS_TABLE).
                where(QueryBuilder.eq("table_id", tableId));


        /*Read the last ID used in the table under analysis.
         If it is still the same, increment it and
         return the new incremented ID.
         Otherwise, read again.
         This technique lets us read and update the ID atomically,
          so it is thread-safe (even it may be if a bit slow).*/
        while (true) {

            /* Get the the last ID used in the table under analysis*/
            long oldCount = CassandraUtils.checkedExecute(session, get).one().getLong("table_counter");

            /* Statement for updating the last ID only if it is still the same*/
            BuiltStatement set = QueryBuilder.update(keyspace, CassandraDatabase.COUNTERS_TABLE).
                    with(QueryBuilder.set("table_counter", (oldCount + 1L))).
                    where(QueryBuilder.eq("table_id", tableId)).
                    onlyIf(QueryBuilder.eq("table_counter", oldCount));

            /* Try to execute the statement; if it succeeded,
            * then it means that no-one has updated the last ID
            * after this thread has read it, so it can use it*/
            if (CassandraUtils.checkedExecute(session, set).wasApplied())
                return oldCount + 1L;
        }
    }
}