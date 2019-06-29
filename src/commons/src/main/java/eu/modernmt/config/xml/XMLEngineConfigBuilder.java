package eu.modernmt.config.xml;

import eu.modernmt.config.*;
import eu.modernmt.lang.Language2;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.lang.LanguageIndex2;
import org.w3c.dom.Element;

/**
 * Created by davide on 04/01/17.
 */
class XMLEngineConfigBuilder extends XMLAbstractBuilder {

    private final XMLDecoderConfigBuilder decoderConfigBuilder;
    private final XMLAlignerConfigBuilder alignerConfigBuilder;
    private final XMLAnalyzerConfigBuilder analyzerConfigBuilder;

    public XMLEngineConfigBuilder(Element element) {
        super(element);
        decoderConfigBuilder = new XMLDecoderConfigBuilder(getChild("decoder"));
        alignerConfigBuilder = new XMLAlignerConfigBuilder(getChild("aligner"));
        analyzerConfigBuilder = new XMLAnalyzerConfigBuilder(getChild("analyzer"));
    }

    public EngineConfig build(EngineConfig config) throws ConfigException {
        if (hasAttribute("source-language") || hasAttribute("target-language")) {
            if (hasAttribute("source-language") && hasAttribute("target-language")) {
                Language2 source = getLanguageAttribute("source-language");
                Language2 target = getLanguageAttribute("target-language");

                LanguageIndex2 languageIndex = new LanguageIndex2.Builder()
                        .add(new LanguageDirection(source, target))
                        .build();

                config.setLanguageIndex(languageIndex);
            } else {
                throw new ConfigException("Missing source/target language specifier in <engine> element");
            }
        } else {
            parseLanguages(getChild("languages"), config);
        }

        if (config.getLanguageIndex() == null)
            throw new ConfigException("Missing language specification for <engine> element");

        decoderConfigBuilder.build(config.getDecoderConfig());
        alignerConfigBuilder.build(config.getAlignerConfig());
        analyzerConfigBuilder.build(config.getAnalyzerConfig());

        return config;
    }

    private static void parseLanguages(Element element, EngineConfig config) throws ConfigException {
        Element[] pairs = getChildren(element, "pair");
        if (pairs == null)
            return;

        LanguageIndex2.Builder builder = null;

        for (Element pair : pairs) {
            if (pair == null)
                continue;

            Language2 source = getLanguageAttribute(pair, "source");
            if (source == null)
                throw new ConfigException("Missing 'source' attribute");

            Language2 target = getLanguageAttribute(pair, "target");
            if (target == null)
                throw new ConfigException("Missing 'target' attribute");

            if (builder == null)
                builder = new LanguageIndex2.Builder();

            builder.add(new LanguageDirection(source, target));
        }

        if (builder != null) {
            parseLanguageRules(getChild(element, "rules"), builder);
            config.setLanguageIndex(builder.build());
        }
    }

    private static void parseLanguageRules(Element element, LanguageIndex2.Builder builder) throws ConfigException {
        Element[] rules = getChildren(element, "rule");
        if (rules == null)
            return;

        for (Element rule : rules) {
            String pattern = getStringAttribute(rule, "match");
            if (pattern == null)
                throw new ConfigException("Missing 'match' attribute");

            Language2 value = getLanguageAttribute(rule, "value");
            if (value == null)
                throw new ConfigException("Missing 'value' attribute");

            try {
                builder.addRule(pattern, value);
            } catch (IllegalArgumentException e) {
                throw new ConfigException("Invalid 'match' attribute: " + pattern, e);
            }
        }
    }

    private static class XMLAlignerConfigBuilder extends XMLAbstractBuilder {

        public XMLAlignerConfigBuilder(Element element) {
            super(element);
        }

        public AlignerConfig build(AlignerConfig config) {
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

            if (hasAttribute("class"))
                config.setDecoderClass(getStringAttribute("class"));

            if (hasAttribute("gpus")) {
                try {
                    config.setGPUs(getIntArrayAttribute("gpus"));
                } catch (IllegalArgumentException e) {
                    throw new ConfigException("Invalid 'gpus' option", e);
                }
            }

            if (config.isUsingGPUs() && hasAttribute("threads"))
                throw new ConfigException("In order to specify 'threads', you have to add gpus='none'");

            return config;
        }
    }

    private static class XMLAnalyzerConfigBuilder extends XMLAbstractBuilder {

        public XMLAnalyzerConfigBuilder(Element element) {
            super(element);
        }

        public AnalyzerConfig build(AnalyzerConfig config) {
            if (hasAttribute("enabled"))
                config.setEnabled(getBooleanAttribute("enabled"));

            if (hasAttribute("analyze"))
                config.setAnalyze(getBooleanAttribute("analyze"));

            if (hasAttribute("batch"))
                config.setBatchSize(getIntAttribute("batch"));

            if (hasAttribute("threads"))
                config.setThreads(getIntAttribute("threads"));

            if (hasAttribute("timeout"))
                config.setTimeout(getIntAttribute("timeout"));

            if (hasAttribute("max-misalignment"))
                config.setMaxToleratedMisalignment(getLongAttribute("max-misalignment"));

            return config;
        }
    }
}
