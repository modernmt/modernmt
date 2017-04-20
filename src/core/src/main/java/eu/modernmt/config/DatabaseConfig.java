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
    /*if the db is enabled MMT start launches a db process or connects to a remote one;
      if it is disabled no db process is launched; MMT does not interact with any DBs;
      By default it is enabled*/
    private boolean enabled = true;
    /*the host of the db process; it may be a name or an IP address
    * by default it is localhost*/
    private String host = "localhost";
    /*the port on which the db process is waiting for clients*/
    private int port = 9042;
    /*the db name*/
    private String name = null;


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

    @Override
    public String toString() {
        return "[Database]\n" +
                "  enabled = " + this.enabled + "\n" +
                "  embedded = " + this.embedded + "\n" +
                "  host = " + this.host + "\n" +
                "  port = " + this.port + "\n" +
                "  name = " + this.name;
    }
}