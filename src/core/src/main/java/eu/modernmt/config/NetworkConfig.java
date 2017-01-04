package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class NetworkConfig {

    private String listeningInterface = null;
    private int port = 5016;
    private int dataPort = 5017;
    private final ApiConfig apiConfig = new ApiConfig();
    private final JoinConfig joinConfig = new JoinConfig();

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

    public int getDataPort() {
        return dataPort;
    }

    public void setDataPort(int dataPort) {
        this.dataPort = dataPort;
    }

    public ApiConfig getApiConfig() {
        return apiConfig;
    }

    public JoinConfig getJoinConfig() {
        return joinConfig;
    }

    @Override
    public String toString() {
        return "[Network]\n" +
                "  interface = " + listeningInterface + "\n" +
                "  port = " + port + "\n" +
                "  data-port = " + dataPort + "\n" +
                "  " + apiConfig.toString().replace("\n", "\n  ") + "\n" +
                "  " + joinConfig.toString().replace("\n", "\n  ");
    }
}
