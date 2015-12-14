package eu.modernmt.engine;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.moses.MosesDecoder;
import eu.modernmt.decoder.moses.MosesINI;
import eu.modernmt.tokenizer.DetokenizerPool;
import eu.modernmt.tokenizer.TokenizerPool;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 27/11/15.
 */
public class TranslationEngine {

    private static final String SYSPROP_ENGINES_PATH = "mmt.engines.path";
    private static final File ENGINES_PATH;

    static {
        String path = System.getProperty(SYSPROP_ENGINES_PATH);
        if (path == null)
            throw new IllegalStateException("The system property '" + SYSPROP_ENGINES_PATH + "' must be initialized to the path of the engines folder.");

        ENGINES_PATH = new File(path);
        if (!ENGINES_PATH.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_ENGINES_PATH + "': " + ENGINES_PATH + " must be a valid directory.");
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
    private Locale targetLanguage;
    private ContextAnalyzer contextAnalyzer;
    private MosesDecoder decoder;
    private File configFile;
    private JSONObject config;

    private TranslationEngine(File engines, String id) throws IOException {
        this.id = id;
        this.root = new File(engines, id);

        if (!root.isDirectory())
            throw new FileNotFoundException(root.toString());

        this.configFile = new File(this.root, ENGINE_CONFIG_PATH);
        if (!configFile.isFile())
            throw new FileNotFoundException(configFile.toString());

        String content = FileUtils.readFileToString(configFile, "UTF-8");
        this.config = new JSONObject(content);
        this.sourceLanguage = Locale.forLanguageTag(config.getJSONObject("engine").getString("source_language"));
        this.targetLanguage = Locale.forLanguageTag(config.getJSONObject("engine").getString("target_language"));
    }

    public void setConfig(JSONObject config) throws IOException {
        this.config = config;

        FileUtils.write(configFile, config.toString(), "UTF-8", false);
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
            Map<String, float[]> weights = getDecoderWeights();
            MosesINI mosesINI = MosesINI.load(new File(this.root, MOSES_INI_PATH));

            if (weights != null) {
                mosesINI.updateWeights(weights);
                mosesINI.save();
            }

            decoder = new MosesDecoder(mosesINI);
        }

        return decoder;
    }

    public TokenizerPool getTokenizer() {
        return TokenizerPool.getCachedInstance(sourceLanguage);
    }

    public DetokenizerPool getDetokenizer() {
        return DetokenizerPool.getCachedInstance(targetLanguage);
    }

    public Locale getSourceLanguage() {
        return sourceLanguage;
    }

    public Locale getTargetLanguage() {
        return targetLanguage;
    }

    public Map<String, float[]> getDecoderWeights() {
        JSONObject json = this.config.optJSONObject("weights");

        if (json == null || json.length() == 0)
            return null;

        HashMap<String, float[]> weights = new HashMap<>();

        for (String key : json.keySet()) {
            JSONArray array = json.getJSONArray(key);

            float[] floats = new float[array.length()];
            for (int i = 0; i < array.length(); i++)
                floats[i] = (float) array.getDouble(i);

            weights.put(key, floats);
        }

        return weights;
    }

    public void setDecoderWeights(Map<String, float[]> weights) {
        if (weights == null || weights.size() == 0)
            this.config.remove("weights");
        else
            this.config.put("weights", weights);

        try {
            FileUtils.write(configFile, config.toString(), "UTF-8", false);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write engine config file");
        }
    }

}
