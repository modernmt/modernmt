package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 * Updated by andrearossi on 03/04/17
 * <p>
 * This class embodies a configuration for a Database,
 * as read from a configuration file (e.g. engineConf.xml)
 * or obtained in some different way
 */
public class DatabaseConfig {

    public enum Type {
        EMBEDDED, STANDALONE
    }

    /*if the db is enabled
        MMT start launches a db process or connects to a remote one;
      if it is disabled
        no db process is launched; MMT does not try to interact with any dbs
      By default it is enabled*/
    private boolean enabled = true;
    /*if the type is EMBEDDED
        EITHER the DB (enabled) is launched on this node, that is LEADER
        OR this node is a FOLLOWER and connects to the DB on another node
      If the type is STANDALONE
        This node connects to separate, remote Kafka server and DB server
        not belonging to any node in the MMT cluster
      BY default, the type is EMBEDDED*/
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

    public void setHost(String host) {
        this.host = host;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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