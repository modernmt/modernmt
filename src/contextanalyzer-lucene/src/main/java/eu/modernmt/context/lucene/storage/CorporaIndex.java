package eu.modernmt.context.lucene.storage;

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

    public static CorporaIndex load(Options analysisOptions, File indexFile, File bucketsFolder) throws IOException {
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
    private final Options analysisOptions;
    private final File bucketsFolder;
    private final HashMap<String, CorpusBucket> bucketById;
    private final HashMap<Long, HashSet<CorpusBucket>> bucketsByMemory;
    private final HashMap<Short, Long> channels;

    public CorporaIndex(File file, Options analysisOptions, File bucketsFolder) {
        this(file, analysisOptions, bucketsFolder, Collections.emptyList(), new HashMap<>());
    }

    private CorporaIndex(File file, Options analysisOptions, File bucketsFolder, Collection<CorpusBucket> buckets, HashMap<Short, Long> channels) {
        this.file = file;
        this.swapFile = new File(file.getParentFile(), "~" + file.getName());
        this.analysisOptions = analysisOptions;
        this.bucketsFolder = bucketsFolder;
        this.channels = channels;

        this.bucketById = new HashMap<>(buckets.size());
        this.bucketsByMemory = new HashMap<>(buckets.size());

        for (CorpusBucket bucket : buckets) {
            this.bucketById.put(bucket.getDocumentId(), bucket);
            this.bucketsByMemory.computeIfAbsent(bucket.getMemory(), k -> new HashSet<>()).add(bucket);
        }

    }

    public boolean shouldAcceptData(short channel, long position) {
        Long existent = this.channels.get(channel);
        return existent == null || position > existent;
    }

    public void advanceChannels(Map<Short, Long> update) {
        for (Map.Entry<Short, Long> entry : update.entrySet()) {
            Short channel = entry.getKey();
            Long position = entry.getValue();
            Long existent = this.channels.get(channel);

            if (existent == null || position > existent)
                this.channels.put(channel, position);
        }
    }

    public CorpusBucket getBucket(UUID owner, String docId) throws IOException {
        return getBucket(owner, docId, true);
    }

    public CorpusBucket getBucket(UUID owner, String docId, boolean createIfAbsent) throws IOException {
        CorpusBucket bucket = bucketById.get(docId);

        if (bucket == null) {
            if (!createIfAbsent)
                return null;

            bucket = new CorpusBucket(analysisOptions, bucketsFolder, owner, docId);

            this.bucketById.put(docId, bucket);
            this.bucketsByMemory.computeIfAbsent(bucket.getMemory(), k -> new HashSet<>()).add(bucket);
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

        bucketById.remove(bucket.getDocumentId());
        HashSet<CorpusBucket> buckets = bucketsByMemory.get(memory);

        if (buckets != null) {
            buckets.remove(bucket);

            if (buckets.isEmpty())
                bucketsByMemory.remove(memory);
        }
    }

    public Collection<CorpusBucket> getBuckets() {
        return bucketById.values();
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

            writer.append(Integer.toString(bucketById.size()));
            writer.append('\n');

            for (CorpusBucket bucket : bucketById.values())
                CorpusBucket.serialize(bucket, writer);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    @Override
    public void close() throws IOException {
        bucketById.values().forEach(IOUtils::closeQuietly);
    }

}
