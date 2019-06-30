package eu.modernmt.decoder.neural;

import eu.modernmt.lang.Language2;
import eu.modernmt.lang.LanguageDirection;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by davide on 02/08/17.
 */
public class ModelConfig {

    private final int DEFAULT_SUGGESTIONS_LIMIT = 1;
    private final int DEFAULT_QUERY_MIN_RESULTS = 10;

    private final HierarchicalINIConfiguration config;
    private final File basePath;

    public static ModelConfig load(File path) throws IOException {
        try {
            return new ModelConfig(new HierarchicalINIConfiguration(path), path.getParentFile());
        } catch (ConfigurationException e) {
            throw new IOException(e);
        }
    }

    private ModelConfig(HierarchicalINIConfiguration config, File basePath) {
        this.config = config;
        this.basePath = basePath;
    }

    public File getBasePath() {
        return basePath;
    }

    public boolean isEchoServer() {
        try {
            SubnodeConfiguration settings = config.configurationAt("settings");
            return settings.getBoolean("echo_server", false);
        } catch (IllegalArgumentException iex) {
            return false;
        }
    }

    public Map<LanguageDirection, File> getAvailableModels() {
        HashMap<LanguageDirection, File> result = new HashMap<>();

        SubnodeConfiguration models;

        try {
            models = config.configurationAt("models");
        } catch (IllegalArgumentException iex) {
            // the passed in key does not map to exactly one node
            return result;
        }

        Iterator<String> keyIterator = models.getKeys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            String[] parts = key.split("__");
            LanguageDirection language = new LanguageDirection(Language2.fromString(parts[0]), Language2.fromString(parts[1]));
            File model = new File(basePath, models.getString(key));

            result.put(language, model);
        }

        return result;
    }

    public int getSuggestionsLimit() {
        try {
            SubnodeConfiguration settings = config.configurationAt("settings");
            return settings.getInt("memory_suggestions_limit", DEFAULT_SUGGESTIONS_LIMIT);
        } catch (IllegalArgumentException iex) {
            return DEFAULT_SUGGESTIONS_LIMIT;
        }
    }

    public int getQueryMinimumResults() {
        try {
            SubnodeConfiguration settings = config.configurationAt("settings");
            return settings.getInt("memory_query_min_results", DEFAULT_QUERY_MIN_RESULTS);
        } catch (IllegalArgumentException iex) {
            return DEFAULT_QUERY_MIN_RESULTS;
        }
    }

}
