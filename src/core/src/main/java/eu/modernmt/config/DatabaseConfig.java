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

    public enum Type {
        EMBEDDED, STANDALONE
    }

    /*if the db is enabled MMT start launches a db process or connects to a remote one;
      if it is disabled no db process is launched; MMT does not interact with any DBs;
      By default it is enabled*/
    private boolean enabled = true;

    /*if the type is EMBEDDED (assuming the DB is enabled):
        - EITHER the DB is launched on this node, that is LEADER;
        - OR this node is a FOLLOWER and connects to the DB on another node;
      If the type is STANDALONE (assuming the DB is enabled):
        This node connects to a separate DB server (not in a MMT cluster node)
      By default, the type is EMBEDDED*/
    private Type type = Type.EMBEDDED;

    /*the host of the db process; it may be a name or an IP address
    * by default it is localhost*/
    private String host = "localhost";

    /*the port on which the db process is waiting for clients*/
    private int port = 9042;

    /*the db name*/
    private String name = null;


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

    @Override
    public String toString() {
        return "[Database]\n" +
                "  enabled = " + this.enabled + "\n" +
                "  type = " + this.type + "\n" +
                "  host = " + this.host + "\n" +
                "  port = " + this.port + "\n" +
                "  name = " + this.name;
    }
}