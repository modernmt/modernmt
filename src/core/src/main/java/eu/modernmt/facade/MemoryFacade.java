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

        try {
            connection = db.getConnection();

            MemoryDAO memoryDAO = db.getMemoryDAO(connection);
            boolean deleted = memoryDAO.delete(id);

            if (!deleted)
                return false;
        } finally {
            IOUtils.closeQuietly(connection);
        }

        BinaryLog binlog = ModernMT.getNode().getBinaryLog();
        binlog.delete(id);

        return true;
    }

    public ImportJob add(LanguageDirection direction, long memoryId, String source, String target) throws BinaryLogException, PersistenceException {
        // Normalizing
        MultilingualCorpus.StringPair pair = new MultilingualCorpus.StringPair(direction, source, target);
        contributionFilter.normalize(pair);

        // Filtering
        if (!contributionFilter.accept(pair, 0))
            return ImportJob.createEphemeralJob(memoryId, 0, BinaryLog.CONTRIBUTIONS_CHANNEL_ID);

        direction = pair.language;
        source = pair.source;
        target = pair.target;

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
            ImportJob job = binlog.upload(direction, memory, source, target, new Date(), BinaryLog.CONTRIBUTIONS_CHANNEL_ID);

            if (job == null)
                return null;

            // Don't store ephemeral ImportJob!

            return job;
        } finally {
            IOUtils.closeQuietly(connection);
        }
    }

    public ImportJob replace(LanguageDirection direction, long memoryId, String sentence, String translation,
                             String previousSentence, String previousTranslation)
            throws BinaryLogException, PersistenceException {
        // Normalizing
        MultilingualCorpus.StringPair previous = new MultilingualCorpus.StringPair(direction, previousSentence, previousTranslation);
        MultilingualCorpus.StringPair current = new MultilingualCorpus.StringPair(direction, sentence, translation);
        contributionFilter.normalize(previous);
        contributionFilter.normalize(current);

        // Filtering
        if (!contributionFilter.accept(current, 0))
            return ImportJob.createEphemeralJob(memoryId, 0, BinaryLog.CONTRIBUTIONS_CHANNEL_ID);

        direction = current.language;
        sentence = current.source;
        translation = current.target;
        previousSentence = previous.source;
        previousTranslation = previous.target;

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
            ImportJob job = binlog.replace(direction, memory, sentence, translation,
                    previousSentence, previousTranslation, new Date(), BinaryLog.CONTRIBUTIONS_CHANNEL_ID);

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
