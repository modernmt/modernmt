package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class NodeConfig {

    private final NetworkConfig networkConfig = new NetworkConfig();
    private final DataStreamConfig dataStreamConfig = new DataStreamConfig();
    private final EngineConfig engineConfig = new EngineConfig();

    public NetworkConfig getNetworkConfig() {
        return networkConfig;
    }

    public DataStreamConfig getDataStreamConfig() {
        return dataStreamConfig;
    }

    public EngineConfig getEngineConfig() {
        return engineConfig;
    }

    @Override
    public String toString() {
        return "[Node]\n" +
                "  " + networkConfig.toString().replace("\n", "\n  ") + "\n" +
                "  " + dataStreamConfig.toString().replace("\n", "\n  ") + "\n" +
                "  " + engineConfig.toString().replace("\n", "\n  ");
    }
}
