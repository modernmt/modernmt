package eu.modernmt.context.lucene.storage;

/**
 * Created by davide on 23/09/16.
 */
public class Options {

    // If set to false, the storage won't invoke analysis of the
    // content. This option can be useful in order to test the storage component.
    public boolean enableAnalysis = true;

    // Number of maximum concurrent analyses in a one batch
    // (one batch is executed every 'writeBehindDelay' milliseconds).
    // If more are present, they are postponed to the next batch.
    public int maxConcurrentAnalyses = 16;

    // Background threads that add corpora to the
    // context analyzer index
    public int analysisThreads = 4;

    // This value controls the maximum write behind delay
    public long writeBehindDelay = 10000L; // 10s (10000 ms)

    // Prevent storing memory in the context analyzer if
    // it has been written less than 'minOffset' bytes
    public long minOffset = 10L * 1024L; // 10kb

    // Force analysis if it has been written at least
    // 'maxToleratedMisalignment' bytes
    public long maxToleratedMisalignment = 5L * 1024L * 1024L; // 5Mb

    // Force analysis if the misalignment between written
    // bytes and analyzed bytes is greater than this value
    public float maxToleratedMisalignmentRatio = 0.5f;

}
