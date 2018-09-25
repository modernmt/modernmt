package eu.modernmt.context.lucene.storage;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CorporaStorage implements DataListener, Closeable {

    private final boolean maskLanguageRegion;
    private final File path;
    private final ConcurrentHashMap<CacheKey, Bucket> buckets = new ConcurrentHashMap<>();
    private final Database db;
    private boolean closed = false;
    private Map<Short, Long> channels;

    public CorporaStorage(File path) throws IOException {
        this(path, true);
    }

    public CorporaStorage(File path, boolean maskLanguageRegion) throws IOException {
        FileUtils.forceMkdir(path);

        this.maskLanguageRegion = maskLanguageRegion;
        this.path = path;
        this.db = new Database(new File(path, "index"));
        this.channels = db.getChannels();
    }

    public int size() throws IOException {
        return db.count();
    }

    public File getPath() {
        return path;
    }

    public Bucket getBucket(long id, LanguagePair language, UUID owner) throws IOException {
        return getBucket(new CacheKey(id, language, this.maskLanguageRegion), owner);
    }

    private Bucket getBucket(CacheKey key, UUID owner) throws IOException {
        try {
            return buckets.computeIfAbsent(key, arg -> {
                try {
                    Bucket bucket = db.retrieve(path, arg.id, arg.language);
                    return bucket == null ? new Bucket(path, arg.id, arg.language, owner) : bucket;
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            });
        } catch (RuntimeIOException e) {
            throw e.getCause();
        }
    }

    private boolean skipData(short channel, long position) {
        Long existent = this.channels.get(channel);
        return existent != null && position <= existent;
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
            if (skipData(unit.channel, unit.channelPosition))
                continue;

            Bucket fwdBucket = getBucket(unit.memory, unit.direction, unit.owner);
            fwdBucket.getWriter().append(unit.rawSentence);
            pendingUpdatesBuckets.add(fwdBucket);

            Bucket bwdBucket = getBucket(unit.memory, unit.direction.reversed(), unit.owner);
            bwdBucket.getWriter().append(unit.rawTranslation);
            pendingUpdatesBuckets.add(bwdBucket);
        }

        for (Deletion deletion : batch.getDeletions()) {
            if (skipData(deletion.channel, deletion.channelPosition))
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

    public Set<Bucket> getUpdatedBuckets(long minMisalignment, int limit) throws IOException {
        return db.retrieveUpdatedBuckets(path, minMisalignment, limit, buckets);
    }

    public void markUpdate(Bucket bucket, long size) throws IOException {
        db.mark(bucket, size);
    }

    static class CacheKey {

        public long id;
        public LanguagePair language;

        public CacheKey(long id, LanguagePair language, boolean maskLanguageRegion) {
            if (maskLanguageRegion) {
                Language owSource = null;
                Language owTarget = null;

                if (language.source.getRegion() != null)
                    owSource = new Language(language.source.getLanguage());
                if (language.target.getRegion() != null)
                    owTarget = new Language(language.target.getLanguage());

                if (owSource != null || owTarget != null) {
                    if (owSource == null)
                        owSource = language.source;
                    if (owTarget == null)
                        owTarget = language.target;

                    language = new LanguagePair(owSource, owTarget);
                }
            }

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
