package eu.modernmt.config.xml;

import eu.modernmt.config.ConfigException;
import eu.modernmt.config.DatabaseConfig;
import eu.modernmt.config.TranslationQueueConfig;
import org.w3c.dom.Element;

class XMLTranslationQueueConfigBuilder extends XMLAbstractBuilder {

    public XMLTranslationQueueConfigBuilder(Element element) {
        super(element);
    }

    public TranslationQueueConfig build(TranslationQueueConfig config) throws ConfigException {
        if (this.hasAttribute("high-priority-size"))
            config.setHighPrioritySize(getIntAttribute("high-priority-size"));
        if (this.hasAttribute("normal-priority-size"))
            config.setNormalPrioritySize(getIntAttribute("normal-priority-size"));
        if (this.hasAttribute("background-priority-size"))
            config.setBackgroundPrioritySize(getIntAttribute("background-priority-size"));

        return config;
    }
}
