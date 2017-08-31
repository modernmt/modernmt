package eu.modernmt.decoder.neural;

import eu.modernmt.lang.LanguagePair;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Created by davide on 02/08/17.
 */
public class ModelConfigFile {

    private final Properties properties;

    public static ModelConfigFile load(File path) throws IOException {
        Properties properties = new Properties();

        Reader reader = null;
        try {
            reader = new FileReader(path);
            properties.load(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return new ModelConfigFile(properties);
    }

    private ModelConfigFile(Properties properties) {
        this.properties = properties;
    }

    public Set<LanguagePair> getAvailableTranslationDirections() {
        HashSet<LanguagePair> result = new HashSet<>();

        for (Object _key : this.properties.keySet()) {
            String key = _key.toString();

            if (key.startsWith("model.")) {
                key = key.substring(6);

                String[] parts = key.split("__");
                result.add(new LanguagePair(Locale.forLanguageTag(parts[0]), Locale.forLanguageTag(parts[1])));
            }
        }

        return result;
    }

    public int getSuggestionsLimit() {
        if (properties.contains("memory_suggestions_limit"))
            return Integer.parseInt(properties.getProperty("memory_suggestions_limit"));
        else
            return 1; // default
    }

    public int getQueryMinimumResults() {
        if (properties.contains("memory_query_min_results"))
            return Integer.parseInt(properties.getProperty("memory_query_min_results"));
        else
            return 10; // default
    }

}
