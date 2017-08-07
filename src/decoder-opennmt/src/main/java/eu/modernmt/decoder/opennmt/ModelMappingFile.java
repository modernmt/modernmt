package eu.modernmt.decoder.opennmt;

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
public class ModelMappingFile {

    public static Set<LanguagePair> readAvailableTranslationDirections(File file) throws IOException {
        Reader reader = null;
        try {
            reader = new FileReader(file);
            return readAvailableTranslationDirections(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public static Set<LanguagePair> readAvailableTranslationDirections(Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);

        HashSet<LanguagePair> result = new HashSet<>();
        for (String key : properties.stringPropertyNames()) {
            String[] parts = key.split("__");
            result.add(new LanguagePair(Locale.forLanguageTag(parts[0]), Locale.forLanguageTag(parts[1])));
        }

        return result;
    }

}
