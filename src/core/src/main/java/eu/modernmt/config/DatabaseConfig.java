package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class DatabaseConfig {

    public enum Type {
        EMBEDDED, STANDALONE
    }

    private boolean enabled = true;
    private Type type = Type.EMBEDDED;
    private String host = "localhost";
    private int port = 9042;

    public Type getType() {
        return this.type;
    }

    public DatabaseConfig setType(Type type) {
        this.type = type;
        return this;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public DatabaseConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public int getPort() {
        return this.port;
    }

    public DatabaseConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String toString() {
        return "[Database]\n" +
                "  enabled = " + this.enabled + "\n" +
                "  type = " + this.type + "\n" +
                "  host = " + this.host + "\n" +
                "  port = " + this.port;
    }
}