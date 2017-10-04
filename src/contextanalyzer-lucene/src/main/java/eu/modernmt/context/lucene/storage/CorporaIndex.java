package eu.modernmt.context.lucene.storage;

import eu.modernmt.lang.LanguagePair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by davide on 22/09/16.
 */
public class CorporaIndex implements Closeable {

    public static CorporaIndex load(Options.AnalysisOptions analysisOptions, File indexFile, File bucketsFolder) throws IOException {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(indexFile));

            // Reading channels

            int length = Integer.parseInt(reader.readLine());
            HashMap<Short, Long> channels = new HashMap<>(length);

            for (int i = 0; i < length; i++) {
                String[] parts = reader.readLine().split(":");
                channels.put(Short.parseShort(parts[0]), Long.parseLong(parts[1]));
            }

            // Reading buckets

            length = Integer.parseInt(reader.readLine());
            ArrayList<CorpusBucket> buckets = new ArrayList<>(length);

            for (int i = 0; i < length; i++) {
                CorpusBucket bucket = CorpusBucket.deserialize(analysisOptions, bucketsFolder, reader);
                buckets.add(bucket);
            }

            // Creating result

            return new CorporaIndex(indexFile, analysisOptions, bucketsFolder, buckets, channels);
        } catch (RuntimeException e) {
            throw new IOException("Invalid index file at " + indexFile, e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    private final File file;
    private final File swapFile;
    private final Options.AnalysisOptions analysisOptions;
    private final File bucketsFolder;
    private final HashMap<BucketKey, CorpusBucket> bucketByKey;
    private final HashMap<Long, HashSet<CorpusBucket>> bucketsByMemory;
    private final HashMap<Short, Long> channels;

    public CorporaIndex(File file, Options.AnalysisOptions analysisOptions, File bucketsFolder) {
        this(file, analysisOptions, bucketsFolder, Collections.emptyList(), new HashMap<>());
    }

    private CorporaIndex(File file, Options.AnalysisOptions analysisOptions, File bucketsFolder, Collection<CorpusBucket> buckets, HashMap<Short, Long> channels) {
        this.file = file;
        this.swapFile = new File(file.getParentFile(), "~" + file.getName());
        this.analysisOptions = analysisOptions;
        this.bucketsFolder = bucketsFolder;
        this.channels = channels;

        this.bucketByKey = new HashMap<>(buckets.size());
        this.bucketsByMemory = new HashMap<>(buckets.size());

        for (CorpusBucket bucket : buckets) {
            BucketKey key = BucketKey.forBucket(bucket);
            this.bucketByKey.put(key, bucket);
            this.bucketsByMemory.computeIfAbsent(bucket.getMemory(), k -> new HashSet<>()).add(bucket);
        }

    }

    public boolean registerData(short channel, long position) {
        Long existent = this.channels.get(channel);

        if (existent == null || position > existent) {
            this.channels.put(channel, position);
            return true;
        } else {
            return false;
        }
    }

    public CorpusBucket getBucket(LanguagePair direction, long memory) throws IOException {
        return getBucket(direction, memory, true);
    }

    public CorpusBucket getBucket(LanguagePair direction, long memory, boolean createIfAbsent) throws IOException {
        BucketKey key = new BucketKey(direction, memory);

        CorpusBucket bucket = bucketByKey.get(key);

        if (bucket == null) {
            if (!createIfAbsent)
                return null;

            bucket = new CorpusBucket(analysisOptions, bucketsFolder, direction, memory);

            this.bucketByKey.put(key, bucket);
            this.bucketsByMemory.computeIfAbsent(memory, k -> new HashSet<>()).add(bucket);
        }

        if (!bucket.isOpen())
            bucket.open();

        return bucket;
    }

    public Collection<CorpusBucket> getBucketsByMemory(long memory) {
        Collection<CorpusBucket> result = this.bucketsByMemory.get(memory);
        return result == null ? Collections.emptyList() : result;
    }

    public void remove(CorpusBucket bucket) {
        Long memory = bucket.getMemory();
        BucketKey key = BucketKey.forBucket(bucket);

        bucketByKey.remove(key);
        HashSet<CorpusBucket> buckets = bucketsByMemory.get(memory);

        if (buckets != null) {
            buckets.remove(bucket);

            if (buckets.isEmpty())
                bucketsByMemory.remove(memory);
        }
    }

    public Collection<CorpusBucket> getBuckets() {
        return bucketByKey.values();
    }

    public synchronized HashMap<Short, Long> getChannels() {
        return new HashMap<>(channels);
    }

    public void save() throws IOException {
        this.store(this.swapFile);
        Files.move(this.swapFile.toPath(), this.file.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
        FileUtils.deleteQuietly(this.swapFile);
    }

    private void store(File path) throws IOException {
        Writer writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(path, false));

            // Writing channels

            writer.append(Integer.toString(channels.size()));
            writer.append('\n');

            for (Map.Entry<Short, Long> channel : channels.entrySet()) {
                writer.append(Short.toString(channel.getKey()));
                writer.append(':');
                writer.append(Long.toString(channel.getValue()));
                writer.append('\n');
            }

            // Writing buckets

            writer.append(Integer.toString(bucketByKey.size()));
            writer.append('\n');

            for (CorpusBucket bucket : bucketByKey.values())
                CorpusBucket.serialize(bucket, writer);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    @Override
    public void close() throws IOException {
        bucketByKey.values().forEach(IOUtils::closeQuietly);
    }

    private static final class BucketKey {

        private final LanguagePair direction;
        private final long memory;

        public static BucketKey forBucket(CorpusBucket bucket) {
            return new BucketKey(bucket.getLanguageDirection(), bucket.getMemory());
        }

        public BucketKey(LanguagePair direction, long memory) {
            this.direction = direction;
            this.memory = memory;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BucketKey bucketKey = (BucketKey) o;

            if (memory != bucketKey.memory) return false;
            return direction.equals(bucketKey.direction);
        }

        @Override
        public int hashCode() {
            int result = direction.hashCode();
            result = 31 * result + (int) (memory ^ (memory >>> 32));
            return result;
        }
    }
}
