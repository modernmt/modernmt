package eu.modernmt.engine;

import eu.modernmt.config.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 27/11/15.
 */
public class TranslationEngine {


    private static final String CONTEXT_ANALYZER_INDEX_PATH = path("models", "context", "index");
    private static final String MOSES_INI_PATH = path("models", "moses.ini");
    private static final String ENGINE_CONFIG_PATH = "engine.ini";

    private static String path(String... path) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < path.length; i++) {
            result.append(path[i]);
            if (i < path.length - 1)
                result.append(File.separatorChar);
        }

        return result.toString();
    }

    private File root;
    private String id;

    private File configFile;
    private File decoderIniTemplate;
    private File caIndexPath;
    private TranslationEngineConfig c;

    public TranslationEngine(String id) throws IOException {
        this.id = id;
        this.root = new File(Config.fs.engines, id);
        this.decoderIniTemplate = new File(this.root, MOSES_INI_PATH);
        this.caIndexPath = new File(this.root, CONTEXT_ANALYZER_INDEX_PATH);
        this.configFile = new File(this.root, ENGINE_CONFIG_PATH);
    }

    public void ensure() throws IOException {
        if (!root.isDirectory())
            throw new FileNotFoundException(root.toString());
        if (!configFile.isFile())
            throw new FileNotFoundException(configFile.toString());
        getConfig();
    }

    private TranslationEngineConfig getConfig() {
        if (c == null) {
            synchronized (this) {
                if (c == null)
                    try {
                        c = TranslationEngineConfig.read(configFile);
                    } catch (TranslationEngineConfig.ConfigException e) {
                        throw new RuntimeException("Unable to load engine config file", e);
                    }
            }
        }

        return c;
    }

    public String getId() {
        return id;
    }

    public Locale getSourceLanguage() {
        return getConfig().getSourceLanguage();
    }

    public Locale getTargetLanguage() {
        return getConfig().getTargetLanguage();
    }

    public File getPath() {
        return root;
    }

    public File getDecoderINITemplate() {
        return decoderIniTemplate;
    }

    public File getContextAnalyzerIndexPath() {
        return caIndexPath;
    }

    public Map<String, float[]> getDecoderWeights() {
        return getConfig().getDecoderWeights();
    }

    public void setDecoderWeights(Map<String, float[]> weights) {
        TranslationEngineConfig config = getConfig();
        config.setDecoderWeights(weights);

        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write engine config file", e);
        }
    }

}
