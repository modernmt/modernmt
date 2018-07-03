package eu.modernmt.config.xml;

import eu.modernmt.config.AlignerConfig;
import eu.modernmt.config.ConfigException;
import eu.modernmt.config.DecoderConfig;
import eu.modernmt.config.EngineConfig;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import org.w3c.dom.Element;

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

                LanguageIndex languageIndex = new LanguageIndex.Builder()
                        .add(new LanguagePair(source, target))
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

        return config;
    }

    private static void parseLanguages(Element element, EngineConfig config) throws ConfigException {
        Element[] pairs = getChildren(element, "pair");
        if (pairs == null)
            return;

        LanguageIndex.Builder builder = null;

        for (Element pair : pairs) {
            if (pair == null)
                continue;

            Language source = getLanguageAttribute(pair, "source");
            if (source == null)
                throw new ConfigException("Missing 'source' attribute");

            Language target = getLanguageAttribute(pair, "target");
            if (target == null)
                throw new ConfigException("Missing 'target' attribute");

            if (builder == null)
                builder = new LanguageIndex.Builder();

            builder.add(new LanguagePair(source, target));
        }

        if (builder != null) {
            parseLanguageRules(getChild(element, "rules"), builder);
            config.setLanguageIndex(builder.build());
        }
    }

    private static void parseLanguageRules(Element element, LanguageIndex.Builder builder) throws ConfigException {
        Element[] rules = getChildren(element, "rule");
        if (rules == null)
            return;

        for (Element rule : rules) {
            Language lang = getLanguageAttribute(rule, "lang");
            if (lang == null)
                throw new ConfigException("Missing 'lang' attribute");

            String _from = getStringAttribute(rule, "from");
            if (_from == null)
                throw new ConfigException("Missing 'from' attribute");
            _from = _from.trim();
            Language from = "*".equals(_from) ? null : Language.fromString(_from);

            Language to = getLanguageAttribute(rule, "to");
            if (to == null)
                throw new ConfigException("Missing 'to' attribute");

            if (from == null)
                builder.addWildcardRule(lang, to);
            else
                builder.addRule(lang, from, to);
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

            if (hasAttribute("class"))
                config.setDecoderClass(getStringAttribute("class"));

            if (hasAttribute("gpus")) {
                try {
                    config.setGPUs(getIntArrayAttribute("gpus"));
                } catch (IllegalArgumentException e) {
                    throw new ConfigException("Invalid 'gpus' option", e);
                }
            }

            return config;
        }
    }
}
