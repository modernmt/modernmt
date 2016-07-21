package eu.modernmt.core.training.partitioning;

import eu.modernmt.io.LineWriter;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by davide on 12/02/16.
 */
public class PartitionWriter implements Closeable {

    private final int size;
    private final Corpus inputCorpus;
    private final CorporaPartition partition;

    private int stored = 0;
    private LineWriter writer = null;

    public PartitionWriter(CorporaPartition partition, Corpus inputCorpus, int size) {
        this.partition = partition;
        this.inputCorpus = inputCorpus;
        this.size = size;
    }

    public boolean write(String line) throws IOException {
        if (stored >= size)
            return false;

        if (writer == null) {
            synchronized (this) {
                if (writer == null) {
                    Corpus outCorpus = partition.getDestinationCorpus(inputCorpus);
                    writer = outCorpus.getContentWriter(false);
                }
            }
        }

        writer.writeLine(line);
        stored++;

        return true;
    }

    public int size() {
        return size;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(writer);
    }

}
