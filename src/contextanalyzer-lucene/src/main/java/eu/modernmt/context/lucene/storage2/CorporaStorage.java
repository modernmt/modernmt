package eu.modernmt.context.lucene.storage2;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.LanguagePair;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CorporaStorage implements DataListener, Closeable {

    private static File getFolder(File path, long id) {
        File parent = new File(path, Long.toString(id % 10000L));
        return new File(parent, Long.toString(id));
    }

    private final File path;
    private final ConcurrentHashMap<CacheKey, Bucket> buckets = new ConcurrentHashMap<>();
    private final Database db;
    private boolean closed = false;
    private Map<Short, Long> channels;

    public CorporaStorage(File path) throws IOException {
        FileUtils.forceMkdir(path);

        this.path = path;
        this.db = new Database(new File(path, "index"));
        this.channels = db.getChannels();
    }

    private Bucket getBucket(long id, LanguagePair language, UUID owner) throws IOException {
        return getBucket(new CacheKey(id, language), owner);
    }

    private Bucket getBucket(CacheKey key, UUID owner) throws IOException {
        try {
            return buckets.computeIfAbsent(key, arg -> {
                try {
                    File folder = getFolder(path, arg.id);
                    FileUtils.forceMkdir(folder);
                    Bucket bucket = db.retrieve(folder, arg.id, arg.language);
                    return bucket == null ? new Bucket(folder, arg.id, arg.language, owner) : bucket;
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            });
        } catch (RuntimeIOException e) {
            throw e.getCause();
        }
    }

    private boolean shouldAcceptData(short channel, long position) {
        Long existent = this.channels.get(channel);
        return existent == null || position > existent;
    }

    private static Map<Short, Long> advanceChannels(Map<Short, Long> channels, Map<Short, Long> update) {
        channels = new HashMap<>(channels);

        for (Map.Entry<Short, Long> entry : update.entrySet()) {
            Short channel = entry.getKey();
            Long position = entry.getValue();
            Long existent = channels.get(channel);

            if (existent == null || position > existent)
                channels.put(channel, position);
        }

        return channels;
    }

    @Override
    public synchronized void onDataReceived(DataBatch batch) throws IOException {
        if (closed)
            return;

        HashSet<Bucket> pendingUpdatesBuckets = new HashSet<>(buckets.size());

        // Apply changes

        for (TranslationUnit unit : batch.getTranslationUnits()) {
            if (!shouldAcceptData(unit.channel, unit.channelPosition))
                continue;

            Bucket fwdBucket = getBucket(unit.memory, unit.direction, unit.owner);
            fwdBucket.getWriter().append(unit.rawSentence);
            pendingUpdatesBuckets.add(fwdBucket);

            Bucket bwdBucket = getBucket(unit.memory, unit.direction.reversed(), unit.owner);
            bwdBucket.getWriter().append(unit.rawTranslation);
            pendingUpdatesBuckets.add(bwdBucket);
        }

        for (Deletion deletion : batch.getDeletions()) {
            if (shouldAcceptData(deletion.channel, deletion.channelPosition))
                continue;

            for (LanguagePair language : db.retrieveLanguages(deletion.memory)) {
                Bucket bucket = getBucket(deletion.memory, language, null);
                bucket.getWriter().delete();
                pendingUpdatesBuckets.add(bucket);
            }
        }

        // Flush pending updates

        for (Bucket bucket : pendingUpdatesBuckets) {
            BucketWriter writer = bucket.getWriter();
            writer.flush();
            writer.close();
        }

        // Update index and finalize

        Map<Short, Long> channels = advanceChannels(this.channels, batch.getChannelPositions());
        db.update(channels, pendingUpdatesBuckets);
        this.channels = channels;

        this.buckets.clear();
    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return Collections.unmodifiableMap(channels);
    }

    @Override
    public boolean needsProcessing() {
        return false;
    }

    @Override
    public boolean needsAlignment() {
        return false;
    }

    @Override
    public synchronized void close() throws IOException {
        closed = true;

        try {
            for (Bucket bucket : buckets.values())
                bucket.getWriter().close();

            buckets.clear();
        } finally {
            db.close();
        }
    }

    private static class CacheKey {

        public long id;
        public LanguagePair language;

        public CacheKey(long id, LanguagePair language) {
            this.id = id;
            this.language = language;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (id != cacheKey.id) return false;
            return language.equals(cacheKey.language);
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + language.hashCode();
            return result;
        }
    }
}
