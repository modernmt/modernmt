package eu.modernmt.config.xml;

import eu.modernmt.config.ApiConfig;
import eu.modernmt.config.ConfigException;
import eu.modernmt.config.JoinConfig;
import eu.modernmt.config.NetworkConfig;
import org.w3c.dom.Element;

/**
 * Created by davide on 04/01/17.
 */
class XMLNetworkConfigBuilder extends XMLAbstractBuilder {

    private final XMLApiConfigBuilder apiConfigBuilder;
    private final XMLJoinConfigBuilder joinConfigBuilder;

    public XMLNetworkConfigBuilder(Element element) {
        super(element);

        apiConfigBuilder = new XMLApiConfigBuilder(getChild("api"));
        joinConfigBuilder = new XMLJoinConfigBuilder(getChild("join"));
    }

    public NetworkConfig build(NetworkConfig config) throws ConfigException {
        if (hasAttribute("host"))
            config.setHost(getStringAttribute("host"));
        if (hasAttribute("port"))
            config.setPort(getIntAttribute("port"));
        if (hasAttribute("interface"))
            config.setListeningInterface(getStringAttribute("interface"));

        apiConfigBuilder.build(config.getApiConfig());
        joinConfigBuilder.build(config.getJoinConfig());

        return config;
    }

    private static class XMLApiConfigBuilder extends XMLAbstractBuilder {

        private XMLApiConfigBuilder(Element element) {
            super(element);
        }

        public ApiConfig build(ApiConfig config) throws ConfigException {
            if (hasAttribute("enabled"))
                config.setEnabled(getBooleanAttribute("enabled"));
            if (hasAttribute("port"))
                config.setPort(getIntAttribute("port"));
            if (hasAttribute("root"))
                config.setApiRoot(getStringAttribute("root"));

            return config;
        }
    }

    private static class XMLJoinConfigBuilder extends XMLAbstractBuilder {

        private XMLJoinConfigBuilder(Element element) {
            super(element);
        }

        public JoinConfig build(JoinConfig config) throws ConfigException {
            Element[] children = getChildren("member");

            if (children != null && children.length > 0) {
                JoinConfig.Member[] members = new JoinConfig.Member[children.length];

                for (int i = 0; i < children.length; i++)
                    members[i] = parseMember(children[i]);

                config.setMembers(members);
            }

            return config;
        }

        private JoinConfig.Member parseMember(Element element) {
            if (element == null)
                return null;

            String host = getStringAttribute(element, "host");
            int port = getIntAttribute(element, "port");

            return new JoinConfig.Member(host, port);
        }

    }
}
