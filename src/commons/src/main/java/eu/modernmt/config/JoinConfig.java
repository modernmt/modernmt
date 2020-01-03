package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class JoinConfig {

    public static class Member {

        private final String host;
        private final int port;

        public Member(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return "Member: " +
                    "host='" + host + '\'' +
                    ", port=" + port;
        }
    }

    private final NetworkConfig parent;
    private int timeout = 5;
    private Member[] members = null;

    public JoinConfig(NetworkConfig parent) {
        this.parent = parent;
    }

    public NetworkConfig getParentConfig() {
        return parent;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Member[] getMembers() {
        return members;
    }

    public void setMembers(Member[] members) {
        this.members = members;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Join: ");
        builder.append("timeout=").append(timeout);
        if (members != null) {
            for (Member member : members)
                builder.append("\n  ").append(member.toString());
        }

        return builder.toString();
    }
}
