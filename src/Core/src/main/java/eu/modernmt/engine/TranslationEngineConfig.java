package eu.modernmt.engine;

import eu.modernmt.decoder.moses.MosesFeature;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 21/12/15.
 */
public class TranslationEngineConfig {

    public static class ConfigException extends IOException {
        public ConfigException(Throwable cause) {
            super(cause);
        }
    }

    public static TranslationEngineConfig read(File file) throws ConfigException {
        try {
            HierarchicalINIConfiguration config = new HierarchicalINIConfiguration(file);
            return new TranslationEngineConfig(config);
        } catch (ConfigurationException e) {
            throw new ConfigException(e);
        }
    }

    private HierarchicalINIConfiguration config;

    private TranslationEngineConfig(HierarchicalINIConfiguration config) {
        this.config = config;
    }

    public Locale getSourceLanguage() {
        SubnodeConfiguration section = config.configurationAt("engine");
        return Locale.forLanguageTag(section.getString("source_lang"));
    }

    public Locale getTargetLanguage() {
        SubnodeConfiguration section = config.configurationAt("engine");
        return Locale.forLanguageTag(section.getString("target_lang"));
    }

    public Map<String, float[]> getDecoderWeights() {
        SubnodeConfiguration section;
        try {
            section = config.configurationAt("weights");
        } catch (IllegalArgumentException e) {
            return null;
        }

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
                    array[i] = MosesFeature.UNTUNEABLE_COMPONENT;
                }
            }

            map.put(feature, array);
        }

        return map;
    }

    public void setDecoderWeights(Map<String, float[]> map) {
        config.clearTree("weights");

        if (map != null) {
            SubnodeConfiguration section = config.getSection("weights");

            for (Map.Entry<String, float[]> entry : map.entrySet()) {
                String feature = entry.getKey();
                float[] array = entry.getValue();

                StringBuilder encoded = new StringBuilder();
                for (int i = 0; i < array.length; i++) {
                    if (array[i] == MosesFeature.UNTUNEABLE_COMPONENT)
                        encoded.append("NULL");
                    else
                        encoded.append(array[i]);

                    if (i < array.length - 1)
                        encoded.append(' ');
                }

                section.setProperty(feature, encoded.toString());
            }
        }
    }

    public void save(File file) throws IOException {
        FileWriter writer = null;

        try {
            writer = new FileWriter(file, false);
            config.save(writer);
        } catch (ConfigurationException e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    @Override
    public String toString() {
        StringWriter out = new StringWriter();

        try {
            config.save(out);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

        return out.toString();
    }
    
}
