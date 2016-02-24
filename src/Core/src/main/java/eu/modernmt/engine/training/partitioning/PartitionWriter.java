package eu.modernmt.engine.training.partitioning;

import eu.modernmt.model.BilingualCorpus;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

/**
 * Created by davide on 12/02/16.
 */
public class PartitionWriter implements Closeable {

    private final int size;
    private final BilingualCorpus inputCorpus;
    private final CorporaPartition partition;

    private int sourceStored;
    private int targetStored;
    private Locale sourceLanguage;
    private Locale targetLanguage;
    private Writer sourceWriter;
    private Writer targetWriter;

    public PartitionWriter(CorporaPartition partition, BilingualCorpus inputCorpus, int size) {
        this.partition = partition;
        this.inputCorpus = inputCorpus;
        this.size = size;
        this.sourceLanguage = inputCorpus.getSourceLanguage();
        this.targetLanguage = inputCorpus.getTargetLanguage();

        this.sourceStored = 0;
        this.targetStored = 0;
        this.sourceWriter = null;
        this.targetWriter = null;
    }

    public boolean write(String line, Locale language) throws IOException {
        if (sourceLanguage.equals(language))
            return writeSource(line);
        else if (targetLanguage.equals(language))
            return writeTarget(line);
        else
            throw new IllegalArgumentException("Unknown language " + language.toLanguageTag());
    }

    private boolean writeSource(String line) throws IOException {
        if (sourceStored >= size)
            return false;

        if (sourceWriter == null) {
            synchronized (this) {
                if (sourceWriter == null) {
                    BilingualCorpus outCorpus = partition.getDestinationParallelCorpus(inputCorpus);
                    sourceWriter = outCorpus.getSourceCorpus().getContentWriter(false);
                }
            }
        }

        sourceWriter.write(line);
        sourceWriter.write('\n');
        sourceStored++;

        return true;
    }

    private boolean writeTarget(String line) throws IOException {
        if (targetStored >= size)
            return false;

        if (targetWriter == null) {
            synchronized (this) {
                if (targetWriter == null) {
                    BilingualCorpus outCorpus = partition.getDestinationParallelCorpus(inputCorpus);
                    targetWriter = outCorpus.getTargetCorpus().getContentWriter(false);
                }
            }
        }

        targetWriter.write(line);
        targetWriter.write('\n');
        targetStored++;

        return true;
    }

    public int size() {
        return size;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(sourceWriter);
        IOUtils.closeQuietly(targetWriter);
    }

}
