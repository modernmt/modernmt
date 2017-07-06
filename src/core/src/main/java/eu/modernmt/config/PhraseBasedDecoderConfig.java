package eu.modernmt.config;

/**
 * Created by davide on 06/07/17.
 */
public class PhraseBasedDecoderConfig extends DecoderConfig {

    private static final int DEFAULT_THREADS;

    static {
        int cores = Runtime.getRuntime().availableProcessors();
        cores = cores > 1 ? (cores * 2) / 3 : cores;

        DEFAULT_THREADS = cores;
    }

    private int threads = DEFAULT_THREADS;

    @Override
    public int getParallelismDegree() {
        return threads;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int thread) {
        this.threads = thread;
    }

    @Override
    public String toString() {
        return "[Phrase-based decoder]\n" +
                "  threads = " + threads + "\n" +
                "  enabled = " + enabled;
    }

}
