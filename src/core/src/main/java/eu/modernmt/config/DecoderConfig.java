package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class DecoderConfig {

    private static final int DEFAULT_THREADS;

    static {
        int cores = Runtime.getRuntime().availableProcessors();
        cores = cores > 1 ? (cores * 2) / 3 : cores;

        DEFAULT_THREADS = cores;
    }

    private boolean enabled = true;
    private int threads = DEFAULT_THREADS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int thread) {
        this.threads = thread;
    }

    @Override
    public String toString() {
        return "[Decoder]\n" +
                "  threads = " + threads + "\n" +
                "  enabled = " + enabled;
    }
}
