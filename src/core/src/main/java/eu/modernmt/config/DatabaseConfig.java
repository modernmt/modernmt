package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class DatabaseConfig {

    public enum Type {
        EMBEDDED, STANDALONE
    }

    private Type type = Type.EMBEDDED;
    private String host = "localhost";
    private int port = 9042;

    public Type getType() {
        return type;
    }

    public DatabaseConfig setType(Type type) {
        this.type = type;
        return this;
    }

    public int getPort() {
        return port;
    }

    public DatabaseConfig setPort(int port) {
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
        return "[Database]\n" +
                "  type = " + type + "\n" +
                "  host = " + host + "\n" +
                "  port = " + port;
    }
}