package eu.modernmt.config.xml;

import eu.modernmt.config.ConfigException;
import eu.modernmt.config.DataStreamConfig;
import org.w3c.dom.Element;

/**
 * Created by davide on 04/01/17.
 */
class XMLDataStreamConfigBuilder extends XMLAbstractBuilder {

    public XMLDataStreamConfigBuilder(Element element) {
        super(element);
    }

    public DataStreamConfig build(DataStreamConfig config) throws ConfigException {
        if (hasAttribute("enabled"))
            config.setEnabled(getBooleanAttribute("enabled"));
        if (hasAttribute("type"))
            config.setType(getEnumAttribute("type", DataStreamConfig.Type.class));
        if (hasAttribute("port"))
            config.setPort(getIntAttribute("port"));

        return config;
    }

}
