package eu.modernmt.cluster.services;

import java.util.Properties;

/**
 * Created by davide on 24/11/17.
 * A TranslationServiceConfig is a wrapper for the Properties object of a TranslationService.
 * It allows to easily get and set the Properties fields for the TranslationService specific values.
 *
 * @see TranslationService
 */
public class TranslationServiceConfig {


    private static final int DEFAULT_QUEUE_SIZE = 1024;

    private final Properties properties;

    public TranslationServiceConfig(Properties properties) {
        this.properties = properties;
    }

    /*NOTE:
    * The getters are typically used by the TranslationService at its creation (its "init" method)
    * The setters are typically used by the ClusterNode when it starts Hazelcast
    * Getters and setters are invoked on different TranslationServiceConfig instances,
    * but they refer to the same Properties object so the side-effects are ensured nonetheless.
    * */

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

    /**
     * Get the amount of threads explicitly set in the Properties for this TranslationService.
     * If no "threads" property was set in the Properties,
     * this method will return as a default value the amount of locally available processors
     * (and the TranslationService will thus use in its ExecutorService a separate thread for each processor)
     * @return the amount of threads to use in the ExecutorService of the TranslationService
     */
    public int getThreads() {
        if (properties.containsKey("threads"))
            return Integer.parseInt(properties.getProperty("threads"));
        else
            return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Get the size of the high priority queue as it is explicitly set in the Properties for this TranslationService.
     * If no "highPriorityQueueSize" property was set in the Properties, this method will return the DEFAULT_QUEUE_SIZE.
     * @return the size to use for the high priority queue in this TranslationExecutor
     */
    public int getHighPriorityQueueSize() {
        if (properties.containsKey("highPriorityQueueSize"))
            return Integer.parseInt(properties.getProperty("highPriorityQueueSize"));
        else
            return DEFAULT_QUEUE_SIZE;
    }

    /**
     * Get the size of the normal priority queue as it is explicitly set in the Properties for this TranslationService.
     * If no "normalPriorityQueueSize" property was set in the Properties, this method will return the DEFAULT_QUEUE_SIZE.
     * @return the size to use for the normal priority queue in this TranslationExecutor
     */
    public int getNormalPriorityQueueSize() {
        if (properties.containsKey("normalPriorityQueueSize"))
            return Integer.parseInt(properties.getProperty("normalPriorityQueueSize"));
        else
            return DEFAULT_QUEUE_SIZE;
    }

    /**
     * Get the size of the background priority queue as it is explicitly set in the Properties for this TranslationService.
     * If no "backgroundPriorityQueueSize" property is set in the Properties this method will return the DEFAULT_QUEUE_SIZE.
     * @return the background to use for the normal priority queue in this TranslationExecutor
     */
    public int getBackgroundPriorityQueueSize() {
        if (properties.containsKey("backgroundPriorityQueueSize"))
            return Integer.parseInt(properties.getProperty("backgroundPriorityQueueSize"));
        else
            return DEFAULT_QUEUE_SIZE;
    }

}
