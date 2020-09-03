package eu.modernmt.decoder.neural;

import eu.modernmt.lang.Language;
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
    private final int DEFAULT_ALTERNATIVES_LIMIT = 20;

    protected final HierarchicalINIConfiguration config;
    protected final File basePath;

    public static ModelConfig load(File path) throws IOException {
        try {
            return new ModelConfig(new HierarchicalINIConfiguration(path), path.getParentFile());
        } catch (ConfigurationException e) {
            throw new IOException(e);
        }
    }

    protected ModelConfig(HierarchicalINIConfiguration config, File basePath) {
        this.config = config;
        this.basePath = basePath;
    }

    public File getBasePath() {
        return basePath;
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
            LanguageDirection language = new LanguageDirection(Language.fromString(parts[0]), Language.fromString(parts[1]));
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


    public int getAlternativesLimit() {
        try {
            SubnodeConfiguration settings = config.configurationAt("translation-settings");
            return settings.getInt("alternatives_limit", DEFAULT_ALTERNATIVES_LIMIT);
        } catch (IllegalArgumentException iex) {
            return DEFAULT_ALTERNATIVES_LIMIT;
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
