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
            return "[Member]\n" +
                    "  host = " + host + "\n" +
                    "  port = " + port;
        }

    }

    private int timeout = 5;
    private Member[] members = null;

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
        StringBuilder builder = new StringBuilder("[Join]\n");
        builder.append("  timeout = ").append(timeout).append('\n');
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
