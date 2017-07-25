package eu.modernmt.context.lucene.storage;

import eu.modernmt.io.DefaultCharset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by davide on 22/09/16.
 */
public class CorpusBucket implements Closeable {

    public static final int SERIALIZED_DATA_LENGTH = 24;

    public static void serialize(CorpusBucket bucket, ByteBuffer buffer) {
        buffer.putLong(bucket.domain);
        buffer.putLong(bucket.analyzerOffset);
        buffer.putLong(bucket.currentOffset);
    }

    public static CorpusBucket deserialize(Options.AnalysisOptions analysisOptions, File folder, ByteBuffer buffer) {
        long domain = buffer.getLong();
        long analyzerOffset = buffer.getLong();
        long currentOffset = buffer.getLong();

        return new CorpusBucket(analysisOptions, folder, domain, analyzerOffset, currentOffset);
    }

    private final Options.AnalysisOptions analysisOptions;

    private final long domain;
    private long analyzerOffset;
    private long currentOffset;
    private boolean deleted;

    private final File path;
    private FileOutputStream stream = null;

    public CorpusBucket(Options.AnalysisOptions analysisOptions, File folder, long domain) {
        this(analysisOptions, folder, domain, 0L, 0L);
    }

    public CorpusBucket(Options.AnalysisOptions analysisOptions, File folder, long domain, long analyzerOffset, long currentOffset) {
        this.domain = domain;
        this.path = new File(folder, "_" + domain);
        this.analysisOptions = analysisOptions;

        this.analyzerOffset = analyzerOffset;
        this.currentOffset = currentOffset;
    }

    public void open() throws IOException {
        if (stream != null)
            throw new IllegalStateException("Bucket already open");

        stream = new FileOutputStream(path, true);

        FileChannel channel = stream.getChannel();
        channel.truncate(currentOffset);
    }

    public boolean isOpen() {
        return stream != null;
    }

    public void append(String line) throws IOException {
        stream.write(line.getBytes(DefaultCharset.get()));
        stream.write('\n');
    }

    public InputStream getContentStream() throws FileNotFoundException {
        return new FileInputStream(path);
    }

    public void flush() throws IOException {
        stream.flush();
        currentOffset = stream.getChannel().position();
    }

    public void delete() throws IOException {
        IOUtils.closeQuietly(stream);
        FileUtils.deleteQuietly(path);

        stream = null;
        analyzerOffset = 0L;
        currentOffset = 0L;
    }

    public long getDomain() {
        return domain;
    }

    public boolean hasUnanalyzedContent() {
        return analyzerOffset < currentOffset;
    }

    public boolean shouldAnalyze() {
        if (deleted)
            return false;

        if (currentOffset < analysisOptions.minOffset)
            return false;

        long notAnalyzedCorpus = currentOffset - analyzerOffset;

        if (notAnalyzedCorpus > analysisOptions.maxToleratedMisalignment)
            return true;

        double ratio = ((double) notAnalyzedCorpus) / currentOffset;

        return ratio > analysisOptions.maxToleratedMisalignmentRatio;
    }

    public void onAnalysisCompleted() {
        analyzerOffset = currentOffset;
    }

    public void markForDeletion() {
        this.deleted = true;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CorpusBucket that = (CorpusBucket) o;

        return domain == that.domain;

    }

    @Override
    public int hashCode() {
        return Long.hashCode(domain);
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            this.flush();

            stream.close();
            stream = null;
        }
    }

}
