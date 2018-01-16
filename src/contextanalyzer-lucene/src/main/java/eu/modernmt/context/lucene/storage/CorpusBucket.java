package eu.modernmt.context.lucene.storage;

import eu.modernmt.io.DefaultCharset;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.channels.FileChannel;

/**
 * Created by davide on 22/09/16.
 */
public class CorpusBucket implements Closeable {

    public static void serialize(CorpusBucket bucket, Writer writer) throws IOException {
        writer.append(Long.toString(bucket.memory));
        writer.append(',');
        writer.append(bucket.direction.source.toLanguageTag());
        writer.append(',');
        writer.append(bucket.direction.target.toLanguageTag());
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

            long memory = Long.parseLong(parts[0]);
            Language source = Language.fromString(parts[1]);
            Language target = Language.fromString(parts[2]);
            long analyzerOffset = Long.parseUnsignedLong(parts[3], 16);
            long currentOffset = Long.parseUnsignedLong(parts[4], 16);

            LanguagePair direction = new LanguagePair(source, target);

            return new CorpusBucket(analysisOptions, folder, direction, memory, analyzerOffset, currentOffset);
        } catch (RuntimeException e) {
            throw new IOException("Unexpected CorpusBucket serialized data: " + line, e);
        }
    }

    private final Options.AnalysisOptions analysisOptions;

    private final long memory;
    private final LanguagePair direction;

    private long analyzerOffset;
    private long currentOffset;
    private boolean deleted;

    private final File path;
    private FileOutputStream stream = null;

    private static String toString(Language locale) {
        return locale.toLanguageTag().replace('-', '_');
    }

    public CorpusBucket(Options.AnalysisOptions analysisOptions, File folder, LanguagePair direction, long memory) {
        this(analysisOptions, folder, direction, memory, 0L, 0L);
    }

    public CorpusBucket(Options.AnalysisOptions analysisOptions, File folder, LanguagePair direction, long memory, long analyzerOffset, long currentOffset) {
        this.direction = direction;
        this.memory = memory;
        this.path = new File(folder, memory + "_" + toString(direction.source) + "__" + toString(direction.target));
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

    public long getMemory() {
        return memory;
    }

    public LanguagePair getLanguageDirection() {
        return direction;
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

        if (memory != that.memory) return false;
        return direction.equals(that.direction);
    }

    @Override
    public int hashCode() {
        int result = (int) (memory ^ (memory >>> 32));
        result = 31 * result + direction.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CorpusBucket{" +
                "memory=" + memory +
                ", direction=" + direction +
                '}';
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
