package eu.modernmt.engine.config;

import eu.modernmt.decoder.DecoderFeature;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by davide on 19/04/16.
 */
public class INIEngineConfigBuilder {

    private File file;

    public INIEngineConfigBuilder(File file) {
        this.file = file;
    }

    public EngineConfig build(String name) throws ConfigException {
        HierarchicalINIConfiguration config;
        try {
            config = new HierarchicalINIConfiguration(file);
        } catch (ConfigurationException e) {
            throw new ConfigException(e);
        }

        EngineConfig engineConfig = new EngineConfig();
        engineConfig.rawConfig = config;

        engineConfig.setName(name);
        readEngineConfig(engineConfig, getSection(config, "engine"));
        readDecoderWeightsConfig(engineConfig.getDecoderConfig(), getSection(config, "weights", null));
        readDecoderConfig(engineConfig.getDecoderConfig(), getSection(config, "moses"));

        return engineConfig;
    }

    private static SubnodeConfiguration getSection(HierarchicalINIConfiguration config, String name) throws ConfigException {
        try {
            return config.configurationAt(name);
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Missing section \"" + name + "\"");
        }
    }

    private static SubnodeConfiguration getSection(HierarchicalINIConfiguration config, String name, SubnodeConfiguration def) {
        try {
            return config.configurationAt(name);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    private static void readEngineConfig(EngineConfig config, SubnodeConfiguration section) {
        Locale source = Locale.forLanguageTag(section.getString("source_lang"));
        Locale target = Locale.forLanguageTag(section.getString("target_lang"));

        config.setSourceLanguage(source);
        config.setTargetLanguage(target);
    }

    private static void readDecoderConfig(DecoderConfig config, SubnodeConfiguration section) {
        int threads = section.getInt("threads", -1);

        if (threads > 0)
            config.setThreads(threads);
    }

    private static void readDecoderWeightsConfig(DecoderConfig config, SubnodeConfiguration section) {
        // TODO: we need to move decoder weights directly into moses, it's not a configuration thing
        if (section != null) {
            HashMap<String, float[]> map = new HashMap<>();
            Iterator<String> keys = section.getKeys();
            while (keys.hasNext()) {
                String feature = keys.next();
                String[] encoded = section.getString(feature).trim().split("\\s+");
                float[] array = new float[encoded.length];

                for (int i = 0; i < encoded.length; i++) {
                    try {
                        array[i] = Float.parseFloat(encoded[i]);
                    } catch (NumberFormatException e) {
                        array[i] = DecoderFeature.UNTUNEABLE_COMPONENT;
                    }
                }

                map.put(feature, array);
            }

            config.setWeights(map);
        }
    }

}
