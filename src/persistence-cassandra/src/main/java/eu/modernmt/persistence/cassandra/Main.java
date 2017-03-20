package eu.modernmt.persistence.cassandra;

import com.datastax.driver.core.Session;
import eu.modernmt.model.Domain;
import eu.modernmt.model.ImportJob;
import eu.modernmt.persistence.DomainDAO;
import eu.modernmt.persistence.ImportJobDAO;
import eu.modernmt.persistence.PersistenceException;
import org.apache.commons.io.IOUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Created by andrea on 09/03/17.
 */
public class Main {
    public static void main(String[] args) throws Throwable {

        CassandraDatabase cassandra = new CassandraDatabase("localhost", 9042);

        System.out.println("dropping");
        cassandra.drop();
        System.out.println("creating");
        cassandra.create();
        System.out.println("testimportjobsdao");
        testImportJobsDao(cassandra);
        System.out.println("aridropping");
        cassandra.drop();
        cassandra.close();
    }


    public static void testDomainDao(CassandraDatabase cassandra) throws PersistenceException {
        CassandraConnection connection = null;

        try {

            connection = (CassandraConnection) cassandra.getConnection(false);
            Session session = connection.session;

            DomainDAO cdd = cassandra.getDomainDAO(connection);
            Domain domain = new Domain(0, "ciao");

            System.out.println(cdd.put(domain));

            System.out.println(cdd.retrieveById(domain.getId()));

            Map<Integer, Domain> map = cdd.retrieveByIds(Collections.singleton(domain.getId()));
            System.out.println(map);

            Collection<Domain> list = cdd.retrieveAll();
            System.out.println(list);

            domain.setName("ciaone");
            System.out.println(cdd.update(domain));

            System.out.println(cdd.retrieveById(domain.getId()));

            map = cdd.retrieveByIds(Collections.singleton(domain.getId()));
            System.out.println(map);

            list = cdd.retrieveAll();
            System.out.println(list);

            System.out.println(cdd.delete(domain.getId()));
            System.out.println(cdd.retrieveById(domain.getId()));

            map = cdd.retrieveByIds(Collections.singleton(domain.getId()));
            System.out.println(map);

            list = cdd.retrieveAll();
            System.out.println(list);


        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public static void testImportJobsDao(CassandraDatabase cassandra) throws PersistenceException {
        CassandraConnection connection = null;

        try {

            connection = (CassandraConnection) cassandra.getConnection(false);
            Session session = connection.session;
            String currentKeyspace = session.getLoggedKeyspace();

            ImportJobDAO cijd = cassandra.getImportJobDAO(connection);
            ImportJob importJob = ImportJob.createEphemeralJob(1, 1L, (short) 1);

            System.out.println(importJob);

            System.out.println(cijd.put(importJob));

            UUID uuid = importJob.getId();
            System.out.println(cijd.retrieveById(uuid));


        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

}
