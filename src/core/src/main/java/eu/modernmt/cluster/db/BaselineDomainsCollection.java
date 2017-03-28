package eu.modernmt.cluster.db;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import eu.modernmt.io.DefaultCharset;
import eu.modernmt.model.Domain;
import eu.modernmt.persistence.Connection;
import eu.modernmt.persistence.Database;
import eu.modernmt.persistence.DomainDAO;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.persistence.cassandra.CassandraDatabase;
import org.apache.commons.io.IOUtils;
import org.apache.xalan.xsltc.DOM;

import java.io.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;

/**
 * Created by andrea on 28/03/17.
 * <p>
 * This class manages the database creation and initialization.
 * It is only employed when a node is performing mmt start
 * and a DB is being build locally (in the same machine).
 */
public class BaselineDomainsCollection {


    private final File domainsJsonPath;

    /**
     * This constructor builds a BaselineDomainsCollection
     * by reading the JSON file with the Domains list
     * and by parsing its Domain objects
     *
     * @param domainsJsonPath the path of the JSON file with the Domains list
     */
    public BaselineDomainsCollection(File domainsJsonPath) {
        this.domainsJsonPath = domainsJsonPath;
    }

    /**
     * This method reads the domains json file and returns
     * the list of domains that it contained
     *
     * @return the list of parsed domains
     */
    public List<Domain> load() {
        Gson gson = new Gson();
        JsonReader jsonReader = null;

        try {
            jsonReader = new JsonReader(
                    new InputStreamReader(
                            new FileInputStream(this.domainsJsonPath),
                            DefaultCharset.get()
                    )
            );
            return gson.fromJson(jsonReader, Domain.class);
        } catch (FileNotFoundException e) {
            return new ArrayList<>(0);
        } finally {
            IOUtils.closeQuietly(jsonReader);
        }

    }
}