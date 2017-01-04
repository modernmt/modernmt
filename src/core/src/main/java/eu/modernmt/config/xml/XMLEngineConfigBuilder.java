package eu.modernmt.config.xml;

import eu.modernmt.config.ConfigException;
import eu.modernmt.config.DecoderConfig;
import eu.modernmt.config.EngineConfig;
import org.w3c.dom.Element;

/**
 * Created by davide on 04/01/17.
 */
class XMLEngineConfigBuilder extends XMLAbstractBuilder {

    private final XMLDecoderConfigBuilder decoderConfigBuilder;

    public XMLEngineConfigBuilder(Element element) {
        super(element);
        decoderConfigBuilder = new XMLDecoderConfigBuilder(getChild("decoder"));
    }

    public EngineConfig build(EngineConfig config) throws ConfigException {
        if (hasAttribute("name"))
            config.setName(getStringAttribute("name"));
        if (hasAttribute("source-language"))
            config.setSourceLanguage(getLocaleAttribute("source-language"));
        if (hasAttribute("target-language"))
            config.setTargetLanguage(getLocaleAttribute("target-language"));

        decoderConfigBuilder.build(config.getDecoderConfig());

        return config;
    }

    private static class XMLDecoderConfigBuilder extends XMLAbstractBuilder {

        public XMLDecoderConfigBuilder(Element element) {
            super(element);
        }

        public DecoderConfig build(DecoderConfig config) throws ConfigException {
            if (hasAttribute("enabled"))
                config.setEnabled(getBooleanAttribute("enabled"));
            if (hasAttribute("threads"))
                config.setThreads(getIntAttribute("threads"));

            return config;
        }
    }
}
