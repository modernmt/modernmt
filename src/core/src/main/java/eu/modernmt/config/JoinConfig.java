package eu.modernmt.config;

/**
 * Created by davide on 04/01/17.
 */
public class JoinConfig {

    public static class Member {

        private final String host;
        private final int port;
        private final int dataPort;

        public Member(String host, int port, int dataPort) {
            this.host = host;
            this.port = port;
            this.dataPort = dataPort;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public int getDataPort() {
            return dataPort;
        }

        @Override
        public String toString() {
            return "[Member]\n" +
                    "  host = " + host + "\n" +
                    "  port = " + port + "\n" +
                    "  data-port = " + dataPort;
        }

    }

    private Member[] members = null;

    public Member[] getMembers() {
        return members;
    }

    public void setMembers(Member[] members) {
        this.members = members;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[Join]\n");
        if (members != null) {
            for (Member member : members) {
                builder.append("  ")
                        .append(member.toString().replace("\n", "\n  "))
                        .append("\n");
            }
        }

        return builder.toString().trim();
    }
}
