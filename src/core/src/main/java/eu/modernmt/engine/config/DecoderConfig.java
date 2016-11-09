package eu.modernmt.engine.config;

import java.util.Map;

/**
 * Created by davide on 19/04/16.
 */
public class DecoderConfig {

    public static final int DEFAULT_THREADS;

    static {
        int cores = Runtime.getRuntime().availableProcessors();

        // Accordingly to "Fast, Scalable Phrase-Based SMT Decoding" [ACL 2016 Submission]
        // current version of Moses Decoder seems to not scale well if number of threads is
        // more than 16.
        cores = cores > 1 ? cores / 2 : cores; // TODO: fast hack to reduce multithread overload

        DEFAULT_THREADS = cores;
    }


    private int threads = DEFAULT_THREADS;
    private Map<String, float[]> weights;

    public Map<String, float[]> getWeights() {
        return weights;
    }

    public void setWeights(Map<String, float[]> weights) {
        this.weights = weights;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

}
