package eu.modernmt.config;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by davide on 04/01/17.
 * Updated by andrearossi on 03/04/17
 * <p>
 * This class embodies a configuration for a BinaryLog.
 * It may be read from a configuration file (e.g. engineConf.xml)
 * (or obtained in some different way)
 */
public class BinaryLogConfig {

    private final NodeConfig parent;
    private boolean enabled = true;
    private boolean embedded = true;
    private String[] hosts = new String[]{"localhost"};
    private int port = 9092;
    private String name = null;

    public BinaryLogConfig(NodeConfig parent) {
        this.parent = parent;
    }

    public NodeConfig getParentConfig() {
        return parent;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public BinaryLogConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isEmbedded() {
        return this.embedded;
    }

    public BinaryLogConfig setEmbedded(boolean embedded) {
        this.embedded = embedded;
        return this;
    }

    public int getPort() {
        return this.port;
    }

    public BinaryLogConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String[] getHosts() {
        return this.hosts;
    }

    public BinaryLogConfig setHost(String host) {
        this.hosts = new String[]{host};
        return this;
    }

    public BinaryLogConfig setHosts(String[] hosts) {
        this.hosts = hosts;
        return this;
    }

    public String getName() {
        return name;
    }

    public BinaryLogConfig setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        return "Binlog: " +
                "enabled=" + enabled +
                ", embedded=" + embedded +
                ", hosts=" + StringUtils.join(hosts, ',') +
                ", port=" + port +
                ", name='" + name + '\'';
    }
}
