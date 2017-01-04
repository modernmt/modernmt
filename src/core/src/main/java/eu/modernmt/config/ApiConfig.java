package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class ApiConfig {

    private boolean enabled = true;
    private String listeningInterface = null;
    private int port = 8045;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getListeningInterface() {
        return listeningInterface;
    }

    public void setListeningInterface(String listeningInterface) {
        this.listeningInterface = listeningInterface;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "[Api]\n" +
                "  enabled = " + enabled + "\n" +
                "  interface = " + listeningInterface + "\n" +
                "  port = " + port;
    }
}
