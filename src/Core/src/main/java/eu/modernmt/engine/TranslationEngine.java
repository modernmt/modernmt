package eu.modernmt.engine;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.moses.MosesDecoder;
import eu.modernmt.decoder.moses.MosesINI;
import eu.modernmt.tokenizer.DetokenizerPool;
import eu.modernmt.tokenizer.TokenizerPool;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 27/11/15.
 */
public class TranslationEngine {

    private static final String CONTEXT_ANALYZER_INDEX_PATH = path("data", "context", "index");
    private static final String MOSES_INI_PATH = path("data", "moses.ini");
    private static final String ENGINE_CONFIG_PATH = "engine.ini";
    private static final String ENGINE_RUNTIME_PATH = "runtime";

    private static final int DEFAULT_DECODER_THREADS;
    private static final int DEFAULT_SA_WORKERS;
    private static final String SYSPROP_ENGINES_PATH = "mmt.engines.path";
    private static final File ENGINES_PATH;

    static {
        // ENGINES_PATH
        String path = System.getProperty(SYSPROP_ENGINES_PATH);
        if (path == null)
            throw new IllegalStateException("The system property '" + SYSPROP_ENGINES_PATH + "' must be initialized to the path of the engines folder.");

        ENGINES_PATH = new File(path);
        if (!ENGINES_PATH.isDirectory())
            throw new IllegalStateException("Invalid path for property '" + SYSPROP_ENGINES_PATH + "': " + ENGINES_PATH + " must be a valid directory.");

        // DEFAULT_DECODER_THREADS and DEFAULT_SA_WORKERS
        int cores = Runtime.getRuntime().availableProcessors();
        DEFAULT_DECODER_THREADS = cores > 3 ? cores - 2 : cores;
        DEFAULT_SA_WORKERS = cores > 3 ? 2 : 1;
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

    private static final ConcurrentHashMap<String, TranslationEngine> engines = new ConcurrentHashMap<>();

    public static TranslationEngine get(String id) throws IOException {
        TranslationEngine engine = new TranslationEngine(ENGINES_PATH, id);
        TranslationEngine existent = engines.putIfAbsent(id, engine);

        return existent == null ? engine : existent;
    }

    private File root;
    private String id;

    private ContextAnalyzer contextAnalyzer;
    private MosesDecoder decoder;
    private File configFile;
    private File runtimePath;
    private TranslationEngineConfig config;

    private TranslationEngine(File engines, String id) throws IOException {
        this.id = id;
        this.root = new File(engines, id);
        this.runtimePath = new File(this.root, ENGINE_RUNTIME_PATH);

        FileUtils.forceMkdir(runtimePath);

        if (!root.isDirectory())
            throw new FileNotFoundException(root.toString());

        this.configFile = new File(this.root, ENGINE_CONFIG_PATH);
        if (!configFile.isFile())
            throw new FileNotFoundException(configFile.toString());

        this.config = TranslationEngineConfig.read(configFile);
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
            File templateFile = new File(this.root, MOSES_INI_PATH);
            MosesINI mosesINI = MosesINI.load(templateFile, this.root);

            if (weights != null)
                mosesINI.setWeights(weights);

            mosesINI.setThreads(getDecoderThreads());
            mosesINI.setWorkers(getSuffixArraysWorkers());


            File inifile = new File(runtimePath, "moses.ini");
            FileUtils.write(inifile, mosesINI.toString(), false);
            decoder = new MosesDecoder(inifile);
        }

        return decoder;
    }

    public TokenizerPool getTokenizer() {
        return TokenizerPool.getCachedInstance(getSourceLanguage());
    }

    public DetokenizerPool getDetokenizer() {
        return DetokenizerPool.getCachedInstance(getTargetLanguage());
    }

    public Locale getSourceLanguage() {
        return config.getSourceLanguage();
    }

    public Locale getTargetLanguage() {
        return config.getTargetLanguage();
    }

    public Map<String, float[]> getDecoderWeights() {
        return config.getDecoderWeights();
    }

    public int getDecoderThreads() {
        return config.getDecoderThreads(DEFAULT_DECODER_THREADS);
    }

    public int getSuffixArraysWorkers() {
        return config.getSuffixArraysWorkers(DEFAULT_SA_WORKERS);
    }

    public void setDecoderWeights(Map<String, float[]> weights) {
        config.setDecoderWeights(weights);

        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write engine config file", e);
        }
    }

}
