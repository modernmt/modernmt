package eu.modernmt.context.lucene.storage;

import eu.modernmt.context.lucene.analysis.DocumentBuilder;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.Objects;

/**
 * Created by davide on 22/09/16.
 */
public class CorpusBucket implements Closeable {

    public static void serialize(CorpusBucket bucket, Writer writer) throws IOException {
        writer.append(bucket.documentId);
        writer.append(',');
        writer.append(Long.toHexString(bucket.analyzerOffset));
        writer.append(',');
        writer.append(Long.toHexString(bucket.currentOffset));
        writer.append('\n');
    }

    public static CorpusBucket deserialize(Options.AnalysisOptions analysisOptions, File folder, BufferedReader reader) throws IOException {
        String line = reader.readLine();

        try {
            String[] parts = line.split(",");

            String documentId = parts[0];
            long analyzerOffset = Long.parseUnsignedLong(parts[1], 16);
            long currentOffset = Long.parseUnsignedLong(parts[2], 16);

            return new CorpusBucket(analysisOptions, folder, documentId, analyzerOffset, currentOffset);
        } catch (RuntimeException e) {
            throw new IOException("Unexpected CorpusBucket serialized data: " + line, e);
        }
    }

    private final Options.AnalysisOptions analysisOptions;

    private final String documentId;
    private final long memory;

    private long analyzerOffset;
    private long currentOffset;
    private boolean deleted;

    private final File path;
    private FileOutputStream stream = null;

    public CorpusBucket(Options.AnalysisOptions analysisOptions, File folder, String documentId) {
        this(analysisOptions, folder, documentId, 0L, 0L);
    }

    public CorpusBucket(Options.AnalysisOptions analysisOptions, File folder, String documentId, long analyzerOffset, long currentOffset) {
        this.documentId = documentId;
        this.memory = DocumentBuilder.getMemory(documentId);
        this.path = new File(folder, documentId);
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
        stream.write(line.getBytes(UTF8Charset.get()));
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

    public long getMemory() {
        return memory;
    }

    public String getDocumentId() {
        return documentId;
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
        return Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId);
    }

    @Override
    public String toString() {
        return "CorpusBucket{" + documentId + "}";
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
