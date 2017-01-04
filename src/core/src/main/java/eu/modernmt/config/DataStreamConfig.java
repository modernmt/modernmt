package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
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
        return type;
    }

    public DataStreamConfig setType(Type type) {
        this.type = type;
        return this;
    }

    public int getPort() {
        return port;
    }

    public DataStreamConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String toString() {
        return "[DataStream]\n" +
                "  enabled = " + enabled + "\n" +
                "  type = " + type + "\n" +
                "  host = " + host + "\n" +
                "  port = " + port;
    }
}
