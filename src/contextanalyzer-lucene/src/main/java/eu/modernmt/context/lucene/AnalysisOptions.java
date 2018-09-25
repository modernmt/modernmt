package eu.modernmt.context.lucene;

/**
 * Created by davide on 23/09/16.
 */
public class AnalysisOptions {

    // If set to false, the analysis won't be invoked.
    // This option can be useful in order to test the storage component.
    public boolean enabled = true;

    // Number of maximum analyses in a one batch
    // (one batch is executed every 'timeout' milliseconds).
    // If more are present, they are postponed to the next batch.
    public int batchSize = 16;

    // Background threads that add corpora to the
    // context analyzer index
    public int threads = 4;

    // This value controls the maximum delay between analysis batches in seconds
    public int timeout = 30;

    // Force analysis if it has been written at least
    // 'maxToleratedMisalignment' bytes
    public long maxToleratedMisalignment = 30L * 1024L; // 30Kb

}
