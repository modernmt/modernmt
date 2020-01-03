package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class AnalyzerConfig {

    private final EngineConfig parent;
    protected boolean enabled = true;

    // If set to false, the analysis won't be invoked.
    // This option can be useful in order to test the storage component.
    protected boolean analyze = true;

    // Number of maximum analyses in a one batch
    // (one batch is executed every 'timeout' milliseconds).
    // If more are present, they are postponed to the next batch.
    protected int batchSize = 16;

    // Background threads that add corpora to the
    // context analyzer index
    protected int threads = 4;

    // This value controls the maximum delay between analysis batches in seconds
    protected int timeout = 30;

    // Force analysis if it has been written at least
    // 'maxToleratedMisalignment' bytes
    protected long maxToleratedMisalignment = 10L * 1024L; // 10Kb

    public AnalyzerConfig(EngineConfig parent) {
        this.parent = parent;
    }

    public EngineConfig getParentConfig() {
        return parent;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean analyze() {
        return analyze;
    }

    public void setAnalyze(boolean analyze) {
        this.analyze = analyze;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public long getMaxToleratedMisalignment() {
        return maxToleratedMisalignment;
    }

    public void setMaxToleratedMisalignment(long maxToleratedMisalignment) {
        this.maxToleratedMisalignment = maxToleratedMisalignment;
    }

    @Override
    public String toString() {
        return "Analyzer: " +
                "enabled=" + enabled +
                ", analyze=" + analyze +
                ", batch=" + batchSize +
                ", threads=" + threads +
                ", timeout=" + timeout +
                ", misalignment=" + maxToleratedMisalignment;
    }
}
