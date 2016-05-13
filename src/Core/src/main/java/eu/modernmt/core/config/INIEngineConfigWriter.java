package eu.modernmt.core.config;

import eu.modernmt.decoder.DecoderFeature;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Created by davide on 19/04/16.
 */
public class INIEngineConfigWriter {

    private HierarchicalINIConfiguration config;

    public INIEngineConfigWriter(EngineConfig config) {
        Map<String, float[]> map = config.getDecoderConfig().getWeights();
        this.config = config.rawConfig;

        this.config.clearTree("weights");

        if (map != null) {
            SubnodeConfiguration section = this.config.getSection("weights");

            for (Map.Entry<String, float[]> entry : map.entrySet()) {
                String feature = entry.getKey();
                float[] array = entry.getValue();

                StringBuilder encoded = new StringBuilder();
                for (int i = 0; i < array.length; i++) {
                    if (array[i] == DecoderFeature.UNTUNEABLE_COMPONENT)
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

    public void write(File file) throws IOException {
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
}
