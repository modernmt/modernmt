package eu.modernmt.facade;

import eu.modernmt.cleaning.ChainedMultilingualCorpusFilter;
import eu.modernmt.cleaning.CorporaCleaning;
import eu.modernmt.cluster.ClusterNode;
import eu.modernmt.cluster.NodeInfo;
import eu.modernmt.data.BinaryLog;
import eu.modernmt.data.BinaryLogException;
import eu.modernmt.data.EmptyCorpusException;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ImportJob;
import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.TranslationUnit;
import eu.modernmt.persistence.*;
import org.apache.commons.io.IOUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by davide on 06/09/16.
 */
public class MemoryFacade {

    private final ChainedMultilingualCorpusFilter contributionFilter = CorporaCleaning.makeMultilingualFilter(
            CorporaCleaning.Options.defaultOptionsForStringPairs());

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
            return memoryDAO.retrieve(id);
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
            return memoryDAO.retrieve(ids);
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public Memory create(UUID owner, String name) throws PersistenceException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            Memory memory = new Memory(0L, owner, name);

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            memory = memoryDAO.store(memory);

            return memory;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public boolean delete(long id) throws PersistenceException, BinaryLogException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        Memory memory;
        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            memory = memoryDAO.retrieve(id);

            if (memory == null)
                return false;

            boolean deleted = memoryDAO.delete(id);

            if (!deleted)
                return false;
        } finally {
            IOUtils.closeQuietly(connection);
        }

        BinaryLog binlog = ModernMT.getNode().getBinaryLog();
        binlog.delete(memory);

        return true;
    }

    public ImportJob add(long memoryId, TranslationUnit tu) throws BinaryLogException, PersistenceException {
        if (tu.timestamp == null)
            tu.timestamp = new Date();

        // Normalizing
        contributionFilter.normalize(tu);

        // Filtering
        if (!contributionFilter.accept(tu, 0))
            return ImportJob.createEphemeralJob(memoryId, 0, BinaryLog.CONTRIBUTIONS_CHANNEL_ID);

        // Adding
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            Memory memory = memoryDAO.retrieve(memoryId);

            if (memory == null)
                return null;

            BinaryLog binlog = ModernMT.getNode().getBinaryLog();
            ImportJob job = binlog.upload(memory, tu, BinaryLog.CONTRIBUTIONS_CHANNEL_ID);

            if (job == null)
                return null;

            // Don't store ephemeral ImportJob!

            return job;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public ImportJob replace(long memoryId, TranslationUnit tu)
            throws BinaryLogException, PersistenceException {
        return replace(memoryId, tu, null);
    }

    public ImportJob replace(long memoryId, TranslationUnit tu, String previousSentence, String previousTranslation)
            throws BinaryLogException, PersistenceException {
        TranslationUnit previous = new TranslationUnit(tu.tuid, tu.language, previousSentence, previousTranslation);
        return replace(memoryId, tu, previous);
    }

    private ImportJob replace(long memoryId, TranslationUnit current, TranslationUnit previous)
            throws BinaryLogException, PersistenceException {
        // Normalizing
        if (previous != null)
            contributionFilter.normalize(previous);
        contributionFilter.normalize(current);

        // Filtering
        if (!contributionFilter.accept(current, 0))
            return ImportJob.createEphemeralJob(memoryId, 0, BinaryLog.CONTRIBUTIONS_CHANNEL_ID);

        // Replacing
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            Memory memory = memoryDAO.retrieve(memoryId);

            if (memory == null)
                return null;

            BinaryLog binlog = ModernMT.getNode().getBinaryLog();
            ImportJob job;
            if (previous == null)
                job = binlog.replace(memory, current, BinaryLog.CONTRIBUTIONS_CHANNEL_ID);
            else
                job = binlog.replace(memory, current, previous.source, previous.target, BinaryLog.CONTRIBUTIONS_CHANNEL_ID);

            if (job == null)
                return null;

            // Don't store ephemeral ImportJob!

            return job;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public ImportJob add(long memoryId, MultilingualCorpus corpus) throws PersistenceException, BinaryLogException {
        Connection connection = null;
        Database db = ModernMT.getNode().getDatabase();

        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            Memory memory = memoryDAO.retrieve(memoryId);

            if (memory == null)
                return null;

            corpus = CorporaCleaning.wrap(corpus, CorporaCleaning.Options.defaultOptionsForMemoryImport());

            BinaryLog binlog = ModernMT.getNode().getBinaryLog();
            ImportJob job = binlog.upload(memory, corpus, BinaryLog.MEMORY_UPLOAD_CHANNEL_ID);

            if (job == null)
                throw new EmptyCorpusException();

            ImportJobDAO jobDAO = db.getImportJobDAO(connection);
            job = jobDAO.store(job);

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
                job = jobDAO.retrieve(id);
            } finally {
                IOUtils.closeQuietly(connection);
            }
        }

        if (job == null)
            return null;

        List<NodeInfo> nodes = ModernMT.getNode().getClusterNodes().stream()
                .filter(node -> node.status == ClusterNode.Status.RUNNING)
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
