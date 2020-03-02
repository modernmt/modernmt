package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 * Updated by andrearossi on 03/04/17
 * <p>
 * This class embodies a configuration for a Database.
 * It may be read from a configuration file (e.g. engineConf.xml)
 * (or obtained in some different way)
 */
public class DatabaseConfig {

    public static final int MYSQL_DEFAULT_PORT = 3306;
    public static final int CASSANDRA_DEFAULT_PORT = 9042;

    public enum Type {CASSANDRA, MYSQL}

    private final NodeConfig parent;
    private boolean embedded = true;
    /*if DB enabled, at start MMT launches/connects to a DB process. If disabled, will not use any DBs. By default: enabled*/
    private boolean enabled = true;

    /*cassandra type: either Cassandra (by default) or MySQL*/
    private Type type = Type.CASSANDRA;
    private int port = CASSANDRA_DEFAULT_PORT;

    /*host and port (default localhost:3306)*/
    private String host = "localhost";
    private String name = null; //the DB name (if mysql) or keyspace name (if Cassandra)

    /*user and password are only used if this is mysql*/
    private String user = null;
    private String password = null;

    public DatabaseConfig(NodeConfig parent) {
        this.parent = parent;
    }

    public NodeConfig getParentConfig() {
        return parent;
    }

    public boolean isEmbedded() {
        return this.embedded;
    }

    public DatabaseConfig setEmbedded(boolean embedded) {
        this.embedded = embedded;
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

    public DatabaseConfig setHost(String host) {
        this.host = host;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public DatabaseConfig setName(String name) {
        this.name = name;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public DatabaseConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Database: " +
                "embedded=" + embedded +
                ", enabled=" + enabled +
                ", type=" + type +
                ", port=" + port +
                ", host='" + host + '\'' +
                ", name='" + name + '\'' +
                ", user='" + user + '\'' +
                ", password='****'";
    }

}