package eu.modernmt.context.lucene.storage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by davide on 22/09/16.
 */
public class CorporaIndex implements Closeable {

    public static CorporaIndex load(Options.AnalysisOptions analysisOptions, File indexFile, File bucketsFolder) throws IOException {
        FileInputStream stream = null;
        byte[] bytes;

        try {
            stream = new FileInputStream(indexFile);
            bytes = IOUtils.toByteArray(stream);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int length = buffer.getInt();

        HashMap<Short, Long> channels = new HashMap<>(length);
        for (short i = 0; i < length; i++) {
            long id = buffer.getLong();

            if (id >= 0)
                channels.put(i, id);
        }

        HashMap<Integer, CorpusBucket> buckets = new HashMap<>();
        while (buffer.hasRemaining()) {
            CorpusBucket bucket = CorpusBucket.deserialize(analysisOptions, bucketsFolder, buffer);
            buckets.put(bucket.getDomain(), bucket);
        }

        return new CorporaIndex(indexFile, analysisOptions, bucketsFolder, buckets, channels);
    }

    private final File file;
    private final File swapFile;
    private final Options.AnalysisOptions analysisOptions;
    private final File bucketsFolder;
    private final HashMap<Integer, CorpusBucket> buckets;
    private final HashMap<Short, Long> channels;

    public CorporaIndex(File file, Options.AnalysisOptions analysisOptions, File bucketsFolder) {
        this(file, analysisOptions, bucketsFolder, new HashMap<>(), new HashMap<>());
    }

    private CorporaIndex(File file, Options.AnalysisOptions analysisOptions, File bucketsFolder, HashMap<Integer, CorpusBucket> buckets, HashMap<Short, Long> channels) {
        this.file = file;
        this.swapFile = new File(file.getParentFile(), "~" + file.getName());
        this.analysisOptions = analysisOptions;
        this.bucketsFolder = bucketsFolder;
        this.buckets = buckets;
        this.channels = channels;
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

    public CorpusBucket getBucket(int domain) {
        return getBucket(domain, true);
    }

    public CorpusBucket getBucket(int domain, boolean computeIfAbsent) {
        if (computeIfAbsent) {
            return buckets.computeIfAbsent(domain,
                    k -> new CorpusBucket(analysisOptions, bucketsFolder, domain)
            );
        } else {
            return buckets.get(domain);
        }
    }

    public CorpusBucket remove(CorpusBucket bucket) {
        return buckets.remove(bucket.getDomain());
    }

    public Collection<CorpusBucket> getBuckets() {
        return buckets.values();
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
        // Compute length
        int maxStreamId = -1;

        for (int id : channels.keySet()) {
            if (id > maxStreamId)
                maxStreamId = id;
        }

        int streamsArrayLength = (maxStreamId < 0 ? 0 : maxStreamId + 1);

        int length = (4 + streamsArrayLength * 8) // channels
                + (buckets.size() * CorpusBucket.SERIALIZED_DATA_LENGTH); // buckets

        // Allocate buffer
        byte[] content = new byte[length];
        ByteBuffer buffer = ByteBuffer.wrap(content);

        // Write channels map
        buffer.putInt(streamsArrayLength);
        if (streamsArrayLength > 0) {
            long[] ids = new long[streamsArrayLength];
            Arrays.fill(ids, -1);

            for (Map.Entry<Short, Long> entry : channels.entrySet()) {
                ids[entry.getKey()] = entry.getValue();
            }

            for (long id : ids) buffer.putLong(id);
        }

        // Write buckets
        for (CorpusBucket bucket : buckets.values())
            CorpusBucket.serialize(bucket, buffer);

        // Write to file
        FileUtils.writeByteArrayToFile(path, content, false);
    }

    @Override
    public void close() throws IOException {
        buckets.values().forEach(IOUtils::closeQuietly);
    }

}
