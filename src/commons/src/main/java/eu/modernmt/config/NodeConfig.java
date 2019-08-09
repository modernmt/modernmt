package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class NodeConfig {

    private final NetworkConfig networkConfig = new NetworkConfig(this);
    private final DataStreamConfig dataStreamConfig = new DataStreamConfig(this);
    private final DatabaseConfig databaseConfig = new DatabaseConfig(this);
    private final EngineConfig engineConfig = new EngineConfig(this);

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

    @Override
    public String toString() {
        return "[Node]\n" +
                "  " + networkConfig.toString().replace("\n", "\n  ") + "\n" +
                "  " + dataStreamConfig.toString().replace("\n", "\n  ") + "\n" +
                "  " + databaseConfig.toString().replace("\n", "\n  ") + "\n" +
                "  " + engineConfig.toString().replace("\n", "\n  ");
    }
}
