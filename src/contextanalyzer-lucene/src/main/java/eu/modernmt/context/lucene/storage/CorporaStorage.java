package eu.modernmt.context.lucene.storage;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.LogDataListener;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class CorporaStorage implements LogDataListener, Closeable {

    protected final File path;
    protected final BucketRegistry buckets;
    private boolean closed = false;
    private final Map<Short, Long> channels;

    public CorporaStorage(File path) throws IOException {
        this(path, true);
    }

    public CorporaStorage(File path, boolean maskLanguageRegion) throws IOException {
        FileUtils.forceMkdir(path);

        this.path = path;
        this.buckets = new BucketRegistry(path, maskLanguageRegion);
        this.channels = buckets.getChannels();
    }

    public int size() throws IOException {
        return buckets.count();
    }

    public Set<Bucket> getUpdatedBuckets(long minMisalignment, int limit) throws IOException {
        return buckets.getUpdated(minMisalignment, limit);
    }

    public void markUpdate(Bucket bucket, long size) throws IOException {
        buckets.mark(bucket, size);
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

        HashSet<Bucket> pendingUpdatesBuckets = new HashSet<>();

        // Apply changes

        for (TranslationUnit unit : batch.getTranslationUnits()) {
            if (skipData(unit.channel, unit.channelPosition))
                continue;

            Bucket fwdBucket = buckets.get(unit.memory, unit.language, unit.owner);
            fwdBucket.getWriter().append(unit.rawSentence);
            pendingUpdatesBuckets.add(fwdBucket);

            Bucket bwdBucket = buckets.get(unit.memory, unit.language.reversed(), unit.owner);
            bwdBucket.getWriter().append(unit.rawTranslation);
            pendingUpdatesBuckets.add(bwdBucket);
        }

        for (Deletion deletion : batch.getDeletions()) {
            if (skipData(deletion.channel, deletion.channelPosition))
                continue;

            for (Bucket bucket : buckets.getAll(deletion.memory)) {
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

        Map<Short, Long> updatedChannels = advanceChannels(channels, batch.getChannelPositions());
        buckets.update(updatedChannels, pendingUpdatesBuckets);
        channels.putAll(updatedChannels);

        buckets.clearCache();
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
        buckets.close();
    }

}
