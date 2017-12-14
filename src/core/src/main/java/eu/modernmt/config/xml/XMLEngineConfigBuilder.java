package eu.modernmt.config.xml;

import eu.modernmt.config.*;
import eu.modernmt.lang.LanguagePair;
import org.w3c.dom.Element;

import java.util.Locale;
import java.util.Set;

/**
 * Created by davide on 04/01/17.
 */
class XMLEngineConfigBuilder extends XMLAbstractBuilder {

    private final XMLDecoderConfigBuilder decoderConfigBuilder;
    private final XMLAlignerConfigBuilder alignerConfigBuilder;

    public XMLEngineConfigBuilder(Element element) {
        super(element);
        decoderConfigBuilder = new XMLDecoderConfigBuilder(getChild("decoder"));
        alignerConfigBuilder = new XMLAlignerConfigBuilder(getChild("aligner"));
    }

    public EngineConfig build(EngineConfig config) throws ConfigException {
        if (hasAttribute("source-language") || hasAttribute("target-language")) {
            if (hasAttribute("source-language") && hasAttribute("target-language")) {
                Locale source = getLocaleAttribute("source-language");
                Locale target = getLocaleAttribute("target-language");

                config.addLanguagePair(new LanguagePair(source, target));
            } else {
                throw new ConfigException("Missing source/target language specifier in <engine> element");
            }
        } else {
            parseLanguages(getChild("languages"), config);
        }

        Set<LanguagePair> pairs = config.getLanguagePairs();
        if (pairs == null || pairs.isEmpty())
            throw new ConfigException("Missing language specification for <engine> element");

        if (hasAttribute("type"))
            config.setType(getEnumAttribute("type", EngineConfig.Type.class));

        decoderConfigBuilder.build(config.getDecoderConfig());
        alignerConfigBuilder.build(config.getAlignerConfig());

        return config;
    }

    private static void parseLanguages(Element element, EngineConfig config) throws ConfigException {
        Element[] children = getChildren(element, "pair");
        if (children == null)
            return;

        for (Element child : children) {
            if (child == null)
                continue;

            Locale source = getLocaleAttribute(child, "source");
            if (source == null)
                throw new ConfigException("Missing 'source' attribute");

            Locale target = getLocaleAttribute(child, "target");
            if (target == null)
                throw new ConfigException("Missing 'target' attribute");

            config.addLanguagePair(new LanguagePair(source, target));
        }
    }

    private static class XMLAlignerConfigBuilder extends XMLAbstractBuilder {

        public XMLAlignerConfigBuilder(Element element) {
            super(element);
        }

        public AlignerConfig build(AlignerConfig config) throws ConfigException {
            if (hasAttribute("enabled"))
                config.setEnabled(getBooleanAttribute("enabled"));

            return config;
        }
    }

    private static class XMLDecoderConfigBuilder extends XMLAbstractBuilder {

        public XMLDecoderConfigBuilder(Element element) {
            super(element);
        }

        public DecoderConfig build(DecoderConfig config) throws ConfigException {

            if (hasAttribute("enabled"))
                config.setEnabled(getBooleanAttribute("enabled"));

            // can not set both gpus and threads
            if (hasAttribute("threads") && hasAttribute("gpus"))
                throw new ConfigException("Decoder can not have both 'threads' element and 'gpus' attributes");

            // (This may be either a phrase-based decoder or a neural decoder config )
            if (hasAttribute("threads"))
                config.setThreads(getIntAttribute("threads"));

            // If 'gpus' is set, this is a neural decoder config
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
