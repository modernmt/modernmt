package eu.modernmt.context.lucene.storage;

import eu.modernmt.context.lucene.analysis.DocumentBuilder;
import eu.modernmt.io.UTF8Charset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by davide on 22/09/16.
 */
public class CorpusBucket implements Closeable {

    public static void serialize(CorpusBucket bucket, Writer writer) throws IOException {
        long ownerMsb = bucket.owner == null ? 0L : bucket.owner.getMostSignificantBits();
        long ownerLsb = bucket.owner == null ? 0L : bucket.owner.getLeastSignificantBits();

        writer.append(bucket.documentId);
        writer.append(',');
        writer.append(Long.toHexString(ownerMsb));
        writer.append(',');
        writer.append(Long.toHexString(ownerLsb));
        writer.append(',');
        writer.append(Long.toHexString(bucket.plainTextFileSize));
        writer.append(',');
        writer.append(Long.toHexString(bucket.virtualFileSize));
        writer.append('\n');
    }

    public static CorpusBucket deserialize(Options analysisOptions, File folder, BufferedReader reader) throws IOException {
        String line = reader.readLine();

        try {
            String[] parts = line.split(",");

            String documentId = parts[0];
            long ownerMsb = Long.parseUnsignedLong(parts[1], 16);
            long ownerLsb = Long.parseUnsignedLong(parts[2], 16);
            long analyzerOffset = Long.parseUnsignedLong(parts[3], 16);
            long plainTextFileSize = Long.parseUnsignedLong(parts[4], 16);
            long virtualFileSize = parts.length > 5 ? Long.parseUnsignedLong(parts[5], 16) : 0L;

            UUID owner = (ownerMsb + ownerLsb) == 0 ? null : new UUID(ownerMsb, ownerLsb);

            return new CorpusBucket(analysisOptions, folder, owner, documentId, analyzerOffset, plainTextFileSize, virtualFileSize);
        } catch (RuntimeException e) {
            throw new IOException("Unexpected CorpusBucket serialized data: " + line, e);
        }
    }

    private final Options analysisOptions;

    private final UUID owner;
    private final String documentId;
    private final long memory;

    private long analyzerOffset;
    private long plainTextFileSize;
    private long virtualFileSize; // size of file including uncompressed bytes size of compressed content
    private boolean deleted;

    private final File path;
    private final File gzPath;
    private FileOutputStream stream = null;

    public CorpusBucket(Options analysisOptions, File folder, UUID owner, String documentId) {
        this(analysisOptions, folder, owner, documentId, 0L, 0L, 0L);
    }

    public CorpusBucket(Options analysisOptions, File folder, UUID owner, String documentId,
                        long analyzerOffset, long plainTextFileSize, long virtualFileSize) {
        this.owner = owner;
        this.documentId = documentId;
        this.memory = DocumentBuilder.getMemory(documentId);
        this.path = new File(folder, documentId);
        this.gzPath = new File(folder, documentId + ".gz");
        this.analysisOptions = analysisOptions;

        this.analyzerOffset = analyzerOffset;
        this.plainTextFileSize = plainTextFileSize;
        this.virtualFileSize = virtualFileSize;
    }

    public void open() throws IOException {
        if (stream != null)
            throw new IllegalStateException("Bucket already open");

        stream = new FileOutputStream(path, true);

        FileChannel channel = stream.getChannel();
        channel.truncate(plainTextFileSize);
    }

    public boolean isOpen() {
        return stream != null;
    }

    public void append(String line) throws IOException {
        stream.write(line.getBytes(UTF8Charset.get()));
        stream.write('\n');
    }

    public InputStream getContentStream() throws IOException {
        boolean success = false;

        InputStream gzStream = null;
        InputStream stream = null;

        try {
            if (gzPath.exists())
                gzStream = new GZIPInputStream(new FileInputStream(gzPath));

            if (path.exists())
                stream = new FileInputStream(path);

            success = true;

            if (gzStream != null && stream != null)
                return new SequenceInputStream(gzStream, stream);
            else if (gzStream != null)
                return gzStream;
            else if (stream != null)
                return stream;
            else
                return new InputStream() {
                    @Override
                    public int read() {
                        return -1;
                    }
                };
        } finally {
            if (!success)
                IOUtils.closeQuietly(gzStream);
        }
    }

    public void flush() throws IOException {
        stream.flush();

        long newSize = stream.getChannel().position();
        virtualFileSize += newSize - plainTextFileSize;
        plainTextFileSize = newSize;
    }

    public void delete() throws IOException {
        IOUtils.closeQuietly(stream);
        FileUtils.deleteQuietly(path);
        FileUtils.deleteQuietly(gzPath);

        stream = null;
        analyzerOffset = 0L;
        virtualFileSize = 0L;
        plainTextFileSize = 0L;
    }

    public UUID getOwner() {
        return owner;
    }

    public long getMemory() {
        return memory;
    }

    public String getDocumentId() {
        return documentId;
    }

    public boolean hasUnanalyzedContent() {
        return analyzerOffset < virtualFileSize;
    }

    public boolean shouldAnalyze() {
        if (deleted)
            return false;

        if (virtualFileSize < analysisOptions.minOffset)
            return false;

        long notAnalyzedCorpus = virtualFileSize - analyzerOffset;

        if (notAnalyzedCorpus > analysisOptions.maxToleratedMisalignment)
            return true;

        double ratio = ((double) notAnalyzedCorpus) / virtualFileSize;

        return ratio > analysisOptions.maxToleratedMisalignmentRatio;
    }

    public void onAnalysisCompleted() {
        analyzerOffset = virtualFileSize;
    }

    public void markForDeletion() {
        this.deleted = true;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean hasUncompressedContent() {
        return this.plainTextFileSize > 0;
    }

    public long getSize() {
        return virtualFileSize;
    }

    public Compression compress() throws IOException {
        this.close();

        File gzSwapFile = new File(gzPath.getParentFile(), gzPath.getName() + ".tmp");
        InputStream input = null;
        OutputStream output = null;

        try {
            input = getContentStream();
            output = new GZIPOutputStream(new FileOutputStream(gzSwapFile, false));

            IOUtils.copy(input, output);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }

        Compression result = new Compression(gzSwapFile, plainTextFileSize);
        plainTextFileSize = 0L;
        return result;
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

    public final class Compression {

        private final File gzSwapFile;
        private final long oldFileSize;

        public Compression(File gzSwapFile, long oldFileSize) {
            this.gzSwapFile = gzSwapFile;
            this.oldFileSize = oldFileSize;
        }

        public CorpusBucket getBucket() {
            return CorpusBucket.this;
        }

        public void commit() throws IOException {
            Files.move(gzSwapFile.toPath(), gzPath.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
            FileUtils.deleteQuietly(path);
        }

        public void rollback() {
            FileUtils.deleteQuietly(gzSwapFile);
            plainTextFileSize = oldFileSize;
        }

    }

}
