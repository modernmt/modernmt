package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class NodeConfig {

    private final NetworkConfig networkConfig = new NetworkConfig();
    private final DataStreamConfig dataStreamConfig = new DataStreamConfig();
    private final DatabaseConfig databaseConfig = new DatabaseConfig();
    private final EngineConfig engineConfig = new EngineConfig();
    private final TranslationQueueConfig translationQueueConfig = new TranslationQueueConfig();

    public NetworkConfig getNetworkConfig() {
        return networkConfig;
    }

    public DataStreamConfig getDataStreamConfig() {
        return dataStreamConfig;
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    public EngineConfig getEngineConfig() {
        return engineConfig;
    }

    public TranslationQueueConfig getTranslationQueueConfig() {
        return translationQueueConfig;
    }

    @Override
    public String toString() {
        return "[Node]\n" +
                "  " + translationQueueConfig.toString().replace("\n", "\n  ") + "\n" +
                "  " + networkConfig.toString().replace("\n", "\n  ") + "\n" +
                "  " + dataStreamConfig.toString().replace("\n", "\n  ") + "\n" +
                "  " + databaseConfig.toString().replace("\n", "\n  ") + "\n" +
                "  " + engineConfig.toString().replace("\n", "\n  ");
    }
}
