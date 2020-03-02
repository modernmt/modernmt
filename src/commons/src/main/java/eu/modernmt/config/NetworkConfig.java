package eu.modernmt.config;

import eu.modernmt.hw.NetworkUtils;

/**
 * Created by davide on 04/01/17.
 */
public class NetworkConfig {

    private final NodeConfig parent;
    private String listeningInterface = NetworkUtils.getMyIpv4Address();
    private int port = 5016;
    private String host = NetworkUtils.getMyIpv4Address();
    private final ApiConfig apiConfig = new ApiConfig(this);
    private final JoinConfig joinConfig = new JoinConfig(this);

    public NetworkConfig(NodeConfig parent) {
        this.parent = parent;
    }

    public NodeConfig getParentConfig() {
        return parent;
    }

    public ApiConfig getApiConfig() {
        return apiConfig;
    }

    public JoinConfig getJoinConfig() {
        return joinConfig;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getListeningInterface() {
        return listeningInterface;
    }

    public void setListeningInterface(String listeningInterface) {
        this.listeningInterface = listeningInterface;
    }

    @Override
    public String toString() {
        return "Network: " +
                "interface='" + listeningInterface + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                "\n  " + apiConfig.toString().replace("\n", "\n  ") +
                "\n  " + joinConfig.toString().replace("\n", "\n  ");
    }
}
