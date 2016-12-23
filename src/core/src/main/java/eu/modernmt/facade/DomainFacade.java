package eu.modernmt.facade;

import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.NodeInfo;
import eu.modernmt.cluster.datastream.DataStreamException;
import eu.modernmt.cluster.datastream.DataStreamManager;
import eu.modernmt.model.Domain;
import eu.modernmt.model.ImportJob;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.persistence.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by davide on 06/09/16.
 */
public class DomainFacade {

    public Collection<Domain> list() throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getEngine().getDatabase();

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
        Database db = ModernMT.getNode().getEngine().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            return domainDAO.retrieveById(domainId);
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
        Database db = ModernMT.getNode().getEngine().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            return domainDAO.retrieveByIds(ids);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Domain create(String name) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getEngine().getDatabase();

        try {
            connection = db.getConnection();

            Domain domain = new Domain(0, name);

            DomainDAO domainDAO = db.getDomainDAO(connection);
            domain = domainDAO.put(domain);

            return domain;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public boolean delete(int id) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getEngine().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            return domainDAO.delete(id);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Domain add(int domainId, String source, String target) throws DataStreamException, PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getEngine().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            Domain domain = domainDAO.retrieveById(domainId);

            if (domain == null)
                return null;

            DataStreamManager manager = ModernMT.getNode().getDataStreamManager();
            manager.upload(domainId, source, target);

            return domain;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public ImportJob add(int domainId, BilingualCorpus corpus) throws PersistenceException, IOException, DataStreamException {
        Connection connection = null;
        Database db = ModernMT.getNode().getEngine().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            Domain domain = domainDAO.retrieveById(domainId);

            if (domain == null)
                return null;

            DataStreamManager manager = ModernMT.getNode().getDataStreamManager();
            ImportJob job = manager.upload(domainId, corpus);

            if (job == null)
                return null;

            ImportJobDAO jobDAO = db.getImportJobDAO(connection);
            job = jobDAO.put(job);

            return job;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public ImportJob getImportJob(int id) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getEngine().getDatabase();

        ImportJob job;

        try {
            connection = db.getConnection();

            ImportJobDAO jobDAO = db.getImportJobDAO(connection);
            job = jobDAO.retrieveById(id);
        } finally {
            IOUtils.closeQuietly(connection);
        }

        if (job == null)
            return null;

        List<NodeInfo> nodes = ModernMT.getNode().getClusterNodes().stream()
                .filter(node -> node.status == ClusterNode.Status.READY)
                .collect(Collectors.toList());

        long begin = job.getBegin();
        long end = job.getEnd();

        long minOffset = Long.MAX_VALUE;
        int completed = 0;

        for (NodeInfo node : nodes) {
            long nodeOffset = node.updatesOffset;

            if (nodeOffset >= end)
                completed++;
            else
                minOffset = Math.min(minOffset, nodeOffset);
        }

        int quota = nodes.size() < 3 ? 1 : Math.round((2.f * nodes.size()) / 3.f);

        if (completed >= quota)
            job.setProgress(1.f);
        else
            job.setProgress(Math.max(0.f, minOffset - begin) / (float) (end - begin));

        return job;
    }

}
