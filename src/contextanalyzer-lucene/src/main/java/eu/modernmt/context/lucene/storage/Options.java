package eu.modernmt.context.lucene.storage;

/**
 * Created by davide on 23/09/16.
 */
public class Options {

    public static Options prepareForBulkLoad() {
        Options options = new Options();
        options.writeBehindDelay = Long.MAX_VALUE;

        options.analysisOptions.minOffset = Long.MAX_VALUE;
        options.analysisOptions.maxToleratedMisalignment = Long.MAX_VALUE;
        options.analysisOptions.maxToleratedMisalignmentRatio = Float.MAX_VALUE;

        return options;
    }

    // Background threads that add corpora to the
    // context analyzer index
    public int analysisThreads = 4;

    // This value controls the maximum write behind delay
    public long writeBehindDelay = 10000L; // 10s (10000 ms)

    public static class AnalysisOptions {
        // Prevent storing domain in the context analyzer if
        // it has been written less than 'minOffset' bytes
        public long minOffset = 10L * 1024L; // 10kb

        // Force analysis if it has been written at least
        // 'maxToleratedMisalignment' bytes
        public long maxToleratedMisalignment = 5L * 1024L * 1024L; // 5Mb

        // Force analysis if the misalignment between written
        // bytes and analyzed bytes is greater than this value
        public float maxToleratedMisalignmentRatio = 0.5f;
    }

    public AnalysisOptions analysisOptions = new AnalysisOptions();

}
