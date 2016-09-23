package eu.modernmt.context.lucene.storage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

        HashMap<Integer, Long> streams = new HashMap<>(length);
        for (int i = 0; i < length; i++) {
            long id = buffer.getLong();

            if (id > 0)
                streams.put(i, id);
        }

        HashMap<Integer, CorpusBucket> buckets = new HashMap<>();
        while (buffer.hasRemaining()) {
            CorpusBucket bucket = CorpusBucket.deserialize(analysisOptions, bucketsFolder, buffer);
            buckets.put(bucket.getDomain(), bucket);
        }

        return new CorporaIndex(analysisOptions, bucketsFolder, buckets, streams);
    }

    private final Options.AnalysisOptions analysisOptions;
    private final File bucketsFolder;
    private final HashMap<Integer, CorpusBucket> buckets;
    private final HashMap<Integer, Long> streams;

    public CorporaIndex(Options.AnalysisOptions analysisOptions, File bucketsFolder) {
        this(analysisOptions, bucketsFolder, new HashMap<>(), new HashMap<>());
    }

    private CorporaIndex(Options.AnalysisOptions analysisOptions, File bucketsFolder, HashMap<Integer, CorpusBucket> buckets, HashMap<Integer, Long> streams) {
        this.analysisOptions = analysisOptions;
        this.bucketsFolder = bucketsFolder;
        this.buckets = buckets;
        this.streams = streams;
    }

    public boolean registerUpdate(int stream, long id) {
        Long existent = this.streams.get(stream);

        if (existent == null || id > existent) {
            this.streams.put(stream, id);
            return true;
        } else {
            return false;
        }
    }

    public CorpusBucket getBucket(int domain) {
        CorpusBucket bucket = buckets.get(domain);

        if (bucket == null) {
            bucket = new CorpusBucket(analysisOptions, bucketsFolder, domain);
            buckets.put(domain, bucket);
        }

        return bucket;
    }

    public Collection<CorpusBucket> getBuckets() {
        return buckets.values();
    }

    public synchronized HashMap<Integer, Long> getStreams() {
        return new HashMap<>(streams);
    }

    public void store(File path) throws IOException {
        // Compute length
        int maxStreamId = -1;

        for (int id : streams.keySet()) {
            if (id > maxStreamId)
                maxStreamId = id;
        }

        int streamsArrayLength = (maxStreamId < 0 ? 0 : maxStreamId + 1);

        int length = (4 + streamsArrayLength * 8) // streams
                + (buckets.size() * CorpusBucket.SERIALIZED_DATA_LENGTH); // buckets

        // Allocate buffer
        byte[] content = new byte[length];
        ByteBuffer buffer = ByteBuffer.wrap(content);

        // Write streams map
        buffer.putInt(streamsArrayLength);
        if (streamsArrayLength > 0) {
            long[] ids = new long[streamsArrayLength];
            for (Map.Entry<Integer, Long> entry : streams.entrySet()) {
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
