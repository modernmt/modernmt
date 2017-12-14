package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public abstract class DecoderConfig {

    protected static final int DEFAULT_THREADS;

    static {
        int cores = Runtime.getRuntime().availableProcessors();
        cores = cores > 1 ? (cores * 2) / 3 : cores;

        DEFAULT_THREADS = cores;
    }

    protected int threads = DEFAULT_THREADS;
    protected boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public abstract int getParallelismDegree();

}
