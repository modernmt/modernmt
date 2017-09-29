package eu.modernmt.facade;

import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.NodeInfo;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.DataManagerException;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ImportJob;
import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.persistence.*;
import org.apache.commons.io.IOUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by davide on 06/09/16.
 */
public class MemoryFacade {

    public Collection<Memory> list() throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            return memoryDAO.retrieveAll();
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Memory get(long id) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            return memoryDAO.retrieveById(id);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Map<Long, Memory> get(long[] ids) throws PersistenceException {
        ArrayList<Long> list = new ArrayList<>(ids.length);
        for (long id : ids)
            list.add(id);

        return get(list);
    }

    public Map<Long, Memory> get(Collection<Long> ids) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            return memoryDAO.retrieveByIds(ids);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Memory create(String name) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            Memory memory = new Memory(0L, name);

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            memory = memoryDAO.put(memory);

            return memory;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public boolean delete(long id) throws PersistenceException, DataManagerException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            boolean deleted = memoryDAO.delete(id);

            if (!deleted)
                return false;
        } finally {
            IOUtils.closeQuietly(connection);
        }

        this.empty(id);

        return true;
    }

    public void empty(long id) throws DataManagerException {
        DataManager dataManager = ModernMT.getNode().getDataManager();
        dataManager.delete(id);
    }

    public ImportJob add(LanguagePair direction, long memoryId, String source, String target) throws DataManagerException, PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            Memory memory = memoryDAO.retrieveById(memoryId);

            if (memory == null)
                return null;

            DataManager dataManager = ModernMT.getNode().getDataManager();
            ImportJob job = dataManager.upload(direction, memoryId, source, target, DataManager.CONTRIBUTIONS_CHANNEL_ID);

            if (job == null)
                return null;

            // Don't store ephemeral ImportJob!

            return job;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public ImportJob add(long memoryId, MultilingualCorpus corpus) throws PersistenceException, DataManagerException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            Memory memory = memoryDAO.retrieveById(memoryId);

            if (memory == null)
                return null;

            DataManager dataManager = ModernMT.getNode().getDataManager();
            ImportJob job = dataManager.upload(memoryId, corpus, DataManager.MEMORY_UPLOAD_CHANNEL_ID);

            if (job == null)
                return null;

            ImportJobDAO jobDAO = db.getImportJobDAO(connection);
            job = jobDAO.put(job);

            return job;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Memory update(Memory memory) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();
            MemoryDAO memoryDAO = db.getMemoryDAO(connection);

            return memoryDAO.update(memory);
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
            Long nodeOffset = node.channels == null ? 0L : node.channels.get(channel);
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
