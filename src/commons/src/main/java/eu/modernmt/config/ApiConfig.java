package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class ApiConfig {

    private final NetworkConfig parent;
    private boolean enabled = true;
    private int port = 8045;
    private String apiRoot = null;

    public ApiConfig(NetworkConfig parent) {
        this.parent = parent;
    }

    public NetworkConfig getParentConfig() {
        return parent;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getApiRoot() {
        return apiRoot;
    }

    public void setApiRoot(String apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public String toString() {
        return "Api: " +
                "enabled=" + enabled +
                ", port=" + port +
                ", root='" + apiRoot + '\'';
    }
}
