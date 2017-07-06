package eu.modernmt.config.xml;

import eu.modernmt.config.*;
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
        if (hasAttribute("source-language"))
            config.setSourceLanguage(getLocaleAttribute("source-language"));
        if (hasAttribute("target-language"))
            config.setTargetLanguage(getLocaleAttribute("target-language"));
        if (hasAttribute("type"))
            config.setType(getEnumAttribute("type", EngineConfig.Type.class));

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

            // Phrase-based decoder config
            if (hasAttribute("threads"))
                pbConfig(config).setThreads(getIntAttribute("threads"));

            // Neural decoder config
            if (hasAttribute("gpus"))
                nConfig(config).setGPUs(getIntArrayAttribute("gpus"));

            return config;
        }

        private static PhraseBasedDecoderConfig pbConfig(DecoderConfig config) throws ConfigException {
            try {
                return (PhraseBasedDecoderConfig) config;
            } catch (ClassCastException e) {
                throw new ConfigException("Decoder is not phrase-based");
            }
        }

        private static NeuralDecoderConfig nConfig(DecoderConfig config) throws ConfigException {
            try {
                return (NeuralDecoderConfig) config;
            } catch (ClassCastException e) {
                throw new ConfigException("Decoder is not neural");
            }
        }
    }
}
