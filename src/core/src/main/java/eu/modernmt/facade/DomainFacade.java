package eu.modernmt.facade;

import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.NodeInfo;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.DataManagerException;
import eu.modernmt.engine.Engine;
import eu.modernmt.model.Domain;
import eu.modernmt.model.ImportJob;
import eu.modernmt.model.LanguagePair;
import eu.modernmt.model.UnsupportedLanguageException;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.persistence.*;
import org.apache.commons.io.IOUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by davide on 06/09/16.
 */
public class DomainFacade {

    public Collection<Domain> list() throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            return domainDAO.retrieveAll();
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Domain get(long domainId) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            return domainDAO.retrieveById(domainId);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Map<Long, Domain> get(long[] ids) throws PersistenceException {
        ArrayList<Long> list = new ArrayList<>(ids.length);
        for (long id : ids)
            list.add(id);

        return get(list);
    }

    public Map<Long, Domain> get(Collection<Long> ids) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

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
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            Domain domain = new Domain(0L, name);

            DomainDAO domainDAO = db.getDomainDAO(connection);
            domain = domainDAO.put(domain);

            return domain;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public boolean delete(long id) throws PersistenceException, DataManagerException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            boolean deleted = domainDAO.delete(id);

            if (!deleted)
                return false;
        } finally {
            IOUtils.closeQuietly(connection);
        }

        DataManager dataManager = ModernMT.getNode().getDataManager();
        dataManager.delete(id);

        return true;
    }

    public ImportJob add(LanguagePair direction, long domainId, String source, String target) throws DataManagerException, PersistenceException {
        Engine engine = ModernMT.getNode().getEngine();

        if (!engine.isLanguagePairSupported(direction))
            throw new UnsupportedLanguageException(direction);

        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            Domain domain = domainDAO.retrieveById(domainId);

            if (domain == null)
                return null;

            DataManager dataManager = ModernMT.getNode().getDataManager();
            ImportJob job = dataManager.upload(direction, domainId, source, target, DataManager.CONTRIBUTIONS_CHANNEL_ID);

            if (job == null)
                return null;

            // Don't store ephemeral ImportJob!

            return job;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public ImportJob add(long domainId, BilingualCorpus corpus) throws PersistenceException, DataManagerException {
        Engine engine = ModernMT.getNode().getEngine();

        LanguagePair direction = new LanguagePair(corpus.getSourceLanguage(), corpus.getTargetLanguage());
        if (!engine.isLanguagePairSupported(direction))
            throw new UnsupportedLanguageException(direction);

        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            DomainDAO domainDAO = db.getDomainDAO(connection);
            Domain domain = domainDAO.retrieveById(domainId);

            if (domain == null)
                return null;

            DataManager dataManager = ModernMT.getNode().getDataManager();
            ImportJob job = dataManager.upload(domainId, corpus, DataManager.DOMAIN_UPLOAD_CHANNEL_ID);

            if (job == null)
                return null;

            ImportJobDAO jobDAO = db.getImportJobDAO(connection);
            job = jobDAO.put(job);

            return job;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public ImportJob getImportJob(UUID id) throws PersistenceException {
        ImportJob job = ImportJob.fromEphemeralUUID(id);

        if (job == null) {
            Connection connection = null;
            Database db = ModernMT.getNode().getDatabase();

            try {
                connection = db.getConnection();

                ImportJobDAO jobDAO = db.getImportJobDAO(connection);
                job = jobDAO.retrieveById(id);
            } finally {
                IOUtils.closeQuietly(connection);
            }
        }

        if (job == null)
            return null;

        List<NodeInfo> nodes = ModernMT.getNode().getClusterNodes().stream()
                .filter(node -> node.status == ClusterNode.Status.READY)
                .collect(Collectors.toList());

        long begin = job.getBegin();
        long end = job.getEnd();
        short channel = job.getDataChannel();

        long minOffset = Long.MAX_VALUE;
        int completed = 0;

        for (NodeInfo node : nodes) {
            Long nodeOffset = node.channelsPositions == null ? 0L : node.channelsPositions.get(channel);
            if (nodeOffset == null)
                nodeOffset = 0L;

            if (nodeOffset >= end)
                completed++;
            else
                minOffset = Math.min(minOffset, nodeOffset);
        }

        int quota = nodes.size() < 3 ? 1 : Math.round((2.f * nodes.size()) / 3.f);

        if (completed >= quota)
            job.setProgress(1.f);
        else if (begin == end)
            job.setProgress(0.f);
        else
            job.setProgress(Math.max(0.f, minOffset - begin) / (float) (end - begin));

        return job;
    }

}
