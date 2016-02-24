package eu.modernmt.engine.training.partitioning;

import eu.modernmt.model.BilingualCorpus;

import java.io.IOException;

/**
 * Created by davide on 11/02/16.
 */
public abstract class CorporaPartition {

    private int size;

    public CorporaPartition() {
        this(0);
    }

    public CorporaPartition(int size) {
        this.size = size;
    }

    public final int getSize() {
        return size;
    }

    public abstract BilingualCorpus getDestinationParallelCorpus(BilingualCorpus sourceCorpus) throws IOException;

}
