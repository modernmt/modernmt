package eu.modernmt.config.xml;

import eu.modernmt.config.ConfigException;
import eu.modernmt.config.DataStreamConfig;
import eu.modernmt.config.DatabaseConfig;
import org.w3c.dom.Element;

/**
 * Created by davide on 04/01/17.
 */
class XMLDatabaseConfigBuilder extends XMLAbstractBuilder {

    public XMLDatabaseConfigBuilder(Element element) {
        super(element);
    }

    public DatabaseConfig build(DatabaseConfig config) throws ConfigException {
        if (hasAttribute("host"))
            config.setHost(getStringAttribute("host"));
        if (hasAttribute("port"))
            config.setPort(getIntAttribute("port"));
        if (hasAttribute("keyspace"))
            config.setPort(getIntAttribute("keyspace"));

        return config;
    }

}
