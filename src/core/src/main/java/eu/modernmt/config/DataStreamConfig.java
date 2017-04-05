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

    public enum Type {
        EMBEDDED, STANDALONE
    }

    private boolean enabled = true;
    private Type type = Type.EMBEDDED;
    private String host = "localhost";
    private int port = 9092;

    public boolean isEnabled() {
        return enabled;
    }

    public DataStreamConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Type getType() {
        return this.type;
    }

    public DataStreamConfig setType(Type type) {
        this.type = type;
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
                "  type = " + this.type + "\n" +
                "  host = " + this.host + "\n" +
                "  port = " + this.port;
    }
}
