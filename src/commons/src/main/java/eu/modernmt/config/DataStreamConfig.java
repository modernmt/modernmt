package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 * Updated by andrearossi on 03/04/17
 * <p>
 * This class embodies a configuration for a DataStream.
 * It may be read from a configuration file (e.g. engineConf.xml)
 * (or obtained in some different way)
 */
public class DataStreamConfig {

    private final NodeConfig parent;
    private boolean enabled = true;
    private boolean embedded = true;
    private String host = "localhost";
    private int port = 9092;
    private String name = null;

    public DataStreamConfig(NodeConfig parent) {
        this.parent = parent;
    }

    public NodeConfig getParentConfig() {
        return parent;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public DataStreamConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isEmbedded() {
        return this.embedded;
    }

    public DataStreamConfig setEmbedded(boolean embedded) {
        this.embedded = embedded;
        return this;
    }

    public int getPort() {
        return this.port;
    }

    public DataStreamConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getHost() {
        return this.host;
    }

    public DataStreamConfig setHost(String host) {
        this.host = host;
        return this;
    }

    @Override
    public String toString() {
        return "[DataStream]\n" +
                "  enabled = " + this.enabled + "\n" +
                "  embedded = " + this.embedded + "\n" +
                "  host = " + this.host + "\n" +
                "  port = " + this.port;
    }

    public String getName() {
        return name;
    }

    public DataStreamConfig setName(String name) {
        this.name = name;
        return this;
    }
}
