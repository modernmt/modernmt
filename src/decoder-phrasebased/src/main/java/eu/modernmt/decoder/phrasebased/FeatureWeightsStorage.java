package eu.modernmt.decoder.phrasebased;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by davide on 04/01/17.
 */
class FeatureWeightsStorage {

    private File file;
    private HashMap<String, float[]> weights;

    public FeatureWeightsStorage(File file) throws IOException {
        this.file = file;

        Properties properties = new Properties();

        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            properties.load(stream);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        this.weights = new HashMap<>(properties.size());

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = entry.getKey().toString();
            String[] values = entry.getValue().toString().split("\\s+");
            float[] floats = new float[values.length];

            for (int i = 0; i < values.length; i++)
                floats[i] = Float.parseFloat(values[i]);

            this.weights.put(name, floats);
        }
    }

    public Map<String, float[]> getWeights() {
        return weights;
    }

    public void setWeights(HashMap<String, float[]> weights) throws IOException {
        this.weights = weights;

        Properties properties = new Properties();
        for (Map.Entry<String, float[]> entry : weights.entrySet()) {
            float[] value = entry.getValue();

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < value.length; i++) {
                if (i > 0)
                    builder.append(' ');
                builder.append(value[i]);
            }

            properties.setProperty(entry.getKey(), builder.toString());
        }

        FileOutputStream stream = null;

        try {
            stream = new FileOutputStream(file, false);
            properties.store(stream, null);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

}
