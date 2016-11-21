package eu.modernmt.facade;

import eu.modernmt.cleaning.Cleaner;
import eu.modernmt.datastream.DataStreamException;
import eu.modernmt.datastream.DataStreamManager;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.persistence.Connection;
import eu.modernmt.persistence.Database;
import eu.modernmt.persistence.DomainDAO;
import eu.modernmt.persistence.PersistenceException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Created by davide on 06/09/16.
 */
public class DomainFacade {

    public Collection<Domain> list() throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.node.getEngine().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            return domainDAO.retrieveAll();
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Domain get(int domainId) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.node.getEngine().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            return domainDAO.retrieveBytId(domainId);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Map<Integer, Domain> get(int[] ids) throws PersistenceException {
        ArrayList<Integer> list = new ArrayList<>(ids.length);
        for (int id : ids)
            list.add(id);

        return get(list);
    }

    public Map<Integer, Domain> get(Collection<Integer> ids) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.node.getEngine().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            return domainDAO.retrieveBytIds(ids);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Domain add(int domainId, BilingualCorpus corpus) throws IOException, DataStreamException, PersistenceException {
        Connection connection = null;
        Database db = ModernMT.node.getEngine().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            Domain domain = domainDAO.retrieveBytId(domainId);

            if (domain == null)
                return null;

            DataStreamManager manager = ModernMT.node.getDataStreamManager();
            manager.upload(domainId, corpus);

            return domain;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Domain add(int domainId, String source, String target) throws DataStreamException, PersistenceException {
        Connection connection = null;
        Database db = ModernMT.node.getEngine().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            Domain domain = domainDAO.retrieveBytId(domainId);

            if (domain == null)
                return null;

            DataStreamManager manager = ModernMT.node.getDataStreamManager();
            manager.upload(domainId, source, target);

            return domain;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Domain create(String name, BilingualCorpus corpus) throws PersistenceException, IOException, DataStreamException {
        corpus = Cleaner.wrap(corpus);

        Connection connection = null;
        Database db = ModernMT.node.getEngine().getDatabase();

        try {
            connection = db.getConnection();

            Domain domain = new Domain(0, name);

            DomainDAO domainDAO = db.getDomainDAO(connection);
            domain = domainDAO.put(domain);

            DataStreamManager manager = ModernMT.node.getDataStreamManager();
            manager.upload(domain.getId(), corpus);

            return domain;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Domain create(String name, String source, String target) throws PersistenceException, DataStreamException {
        Connection connection = null;
        Database db = ModernMT.node.getEngine().getDatabase();

        try {
            connection = db.getConnection();

            Domain domain = new Domain(0, name);

            DomainDAO domainDAO = db.getDomainDAO(connection);
            domain = domainDAO.put(domain);

            DataStreamManager manager = ModernMT.node.getDataStreamManager();
            manager.upload(domain.getId(), source, target);

            return domain;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

}
