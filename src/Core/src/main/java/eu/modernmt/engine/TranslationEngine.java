package eu.modernmt.engine;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.moses.MosesDecoder;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 27/11/15.
 */
public class TranslationEngine {

    private static final File ENGINES_PATH;

    static {
        String path = System.getProperty("mmt.engines.path");
        if (path == null)
            throw new IllegalStateException("The system property 'mmt.engines.path' must be initialized to the path of the engines folder.");

        ENGINES_PATH = new File(path);
        if (!ENGINES_PATH.isDirectory())
            throw new IllegalStateException("Invalid path for property 'mmt.engines.path': " + ENGINES_PATH + " must be a valid directory.");
    }

    private static String path(String... path) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < path.length; i++) {
            result.append(path[i]);
            if (i < path.length - 1)
                result.append(File.separatorChar);
        }

        return result.toString();
    }

    private static final String CONTEXT_ANALYZER_INDEX_PATH = path("data", "context", "index");
    private static final String MOSES_INI_PATH = path("data", "moses.ini");
    private static final String ENGINE_CONFIG_PATH = "engine.json";

    private static final ConcurrentHashMap<String, TranslationEngine> engines = new ConcurrentHashMap<>();

    public static TranslationEngine get(String id) throws IOException {
        TranslationEngine engine = new TranslationEngine(ENGINES_PATH, id);
        TranslationEngine existent = engines.putIfAbsent(id, engine);

        return existent == null ? engine : existent;
    }

    private File root;
    private String id;

    private Locale sourceLanguage;
    private ContextAnalyzer contextAnalyzer;
    private MosesDecoder decoder;

    private TranslationEngine(File engines, String id) throws IOException {
        this.id = id;
        this.root = new File(engines, id);

        if (!root.isDirectory())
            throw new FileNotFoundException(root.toString());

        File config = new File(this.root, ENGINE_CONFIG_PATH);
        if (!config.isFile())
            throw new FileNotFoundException(config.toString());

        String content = FileUtils.readFileToString(config, "UTF-8");
        JSONObject json = new JSONObject(content);
        this.sourceLanguage = Locale.forLanguageTag(json.getJSONObject("engine").getString("source_language"));
    }

    public String getId() {
        return id;
    }

    public ContextAnalyzer getContextAnalyzer() throws IOException {
        if (contextAnalyzer == null) {
            File indexPath = new File(this.root, CONTEXT_ANALYZER_INDEX_PATH);
            contextAnalyzer = new ContextAnalyzer(indexPath);
        }

        return contextAnalyzer;
    }

    public Decoder getDecoder() throws IOException {
        if (decoder == null) {
            File mosesIni = new File(this.root, MOSES_INI_PATH);
            decoder = new MosesDecoder(mosesIni);
        }

        return decoder;
    }

    public Locale getSourceLanguage() {
        return sourceLanguage;
    }
}
