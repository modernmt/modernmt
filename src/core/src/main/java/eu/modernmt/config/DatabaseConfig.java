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

    private boolean embedded = true;
    /*if DB enabled, at start MMT launches/connects to a DB process. If disabled, will not use any DBs. By default: enabled*/
    private boolean enabled = true;

    /*host and port (default localhost:3306)*/
    private String host = "localhost";
    private int port = 3306;

    private String name = null;
    private String user = null;
    private String password = null;


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

    @Override
    public String toString() {
        return "[Database]\n{" +
                "embedded=" + embedded +
                ", enabled=" + enabled +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", name='" + name + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}