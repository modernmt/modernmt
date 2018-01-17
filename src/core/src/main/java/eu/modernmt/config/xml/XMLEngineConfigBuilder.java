package eu.modernmt.config.xml;

import eu.modernmt.config.*;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import org.w3c.dom.Element;

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
                Language source = getLanguageAttribute("source-language");
                Language target = getLanguageAttribute("target-language");

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

            Language source = getLanguageAttribute(child, "source");
            if (source == null)
                throw new ConfigException("Missing 'source' attribute");

            Language target = getLanguageAttribute(child, "target");
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

            if (hasAttribute("threads"))
                config.setThreads(getIntAttribute("threads"));

            if (config instanceof NeuralDecoderConfig) {
                NeuralDecoderConfig neuralConfig = (NeuralDecoderConfig) config;
                if (hasAttribute("gpus"))
                    neuralConfig.setGPUs(getIntArrayAttribute("gpus"));

                if (neuralConfig.isUsingGPUs() && hasAttribute("threads"))
                    throw new ConfigException("In order to specify 'threads', you have to add gpus='none'");
            }

            return config;
        }
    }
}
