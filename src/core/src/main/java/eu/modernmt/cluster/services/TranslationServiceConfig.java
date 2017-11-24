package eu.modernmt.cluster.services;

import java.util.Properties;

/**
 * Created by davide on 24/11/17.
 */
public class TranslationServiceConfig {

    private static final int DEFAULT_QUEUE_SIZE = 1024;

    private final Properties properties;

    public TranslationServiceConfig(Properties properties) {
        this.properties = properties;
    }

    public TranslationServiceConfig setThreads(int threads) {
        properties.setProperty("threads", Integer.toString(threads));
        return this;
    }

    public TranslationServiceConfig setHighPriorityQueueSize(int highPriorityQueueSize) {
        properties.setProperty("highPriorityQueueSize", Integer.toString(highPriorityQueueSize));
        return this;
    }

    public TranslationServiceConfig setNormalPriorityQueueSize(int normalPriorityQueueSize) {
        properties.setProperty("normalPriorityQueueSize", Integer.toString(normalPriorityQueueSize));
        return this;
    }

    public TranslationServiceConfig setBackgroundPriorityQueueSize(int backgroundPriorityQueueSize) {
        properties.setProperty("backgroundPriorityQueueSize", Integer.toString(backgroundPriorityQueueSize));
        return this;
    }

    public int getThreads() {
        if (properties.containsKey("threads"))
            return Integer.parseInt(properties.getProperty("threads"));
        else
            return Runtime.getRuntime().availableProcessors();
    }

    public int getHighPriorityQueueSize() {
        if (properties.containsKey("highPriorityQueueSize"))
            return Integer.parseInt(properties.getProperty("highPriorityQueueSize"));
        else
            return DEFAULT_QUEUE_SIZE;
    }

    public int getNormalPriorityQueueSize() {
        if (properties.containsKey("normalPriorityQueueSize"))
            return Integer.parseInt(properties.getProperty("normalPriorityQueueSize"));
        else
            return DEFAULT_QUEUE_SIZE;
    }

    public int getBackgroundPriorityQueueSize() {
        if (properties.containsKey("backgroundPriorityQueueSize"))
            return Integer.parseInt(properties.getProperty("backgroundPriorityQueueSize"));
        else
            return DEFAULT_QUEUE_SIZE;
    }

}
