package eu.modernmt.decoder.neural;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 02/08/17.
 */
public class ModelConfig {

    private final int DEFAULT_SUGGESTIONS_LIMIT = 1;
    private final int DEFAULT_QUERY_MIN_RESULTS = 10;

    private final HierarchicalINIConfiguration config;

    public static ModelConfig load(File path) throws IOException {
        try {
            return new ModelConfig(new HierarchicalINIConfiguration(path));
        } catch (ConfigurationException e) {
            throw new IOException(e);
        }
    }

    private ModelConfig(HierarchicalINIConfiguration config) {
        this.config = config;
    }

    public boolean isEchoServer() {
        try {
            SubnodeConfiguration settings = config.configurationAt("settings");
            return settings.getBoolean("echo_server", false);
        } catch (IllegalArgumentException iex) {
            return false;
        }
    }

    public Set<LanguagePair> getAvailableTranslationDirections() {
        HashSet<LanguagePair> result = new HashSet<>();

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
            result.add(new LanguagePair(Language.fromString(parts[0]), Language.fromString(parts[1])));
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

    public Map<LanguagePair, Float> getAlignmentThresholds() {
        SubnodeConfiguration thresholds;

        try {
            thresholds = config.configurationAt("alignment-filter");
        } catch (IllegalArgumentException iex) {
            // the passed in key does not map to exactly one node
            return null;
        }

        Map<LanguagePair, Float> result = new HashMap<>();

        Iterator<String> keyIterator = thresholds.getKeys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            float threshold = thresholds.getFloat(key);

            String[] parts = key.split("__");
            LanguagePair pair = new LanguagePair(Language.fromString(parts[0]), Language.fromString(parts[1]));

            result.put(pair, threshold);
        }

        return result;
    }
}
