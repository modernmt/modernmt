package eu.modernmt.core;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.fastalign.SymmetrizedAligner;
import eu.modernmt.config.Config;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.core.config.EngineConfig;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.moses.MosesDecoder;
import eu.modernmt.decoder.moses.MosesINI;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

/**
 * Created by davide on 19/04/16.
 */
public class Engine {

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

    private final EngineConfig config;
    private final int threads;
    private final File root;
    private final File decoderIniTemplatePath;
    private final File caIndexPath;
    private final File configFile;

    private File workingDirectory = new File(System.getProperty("user.dir"));

    private Decoder decoder = null;
    private Aligner aligner = null;
    private Preprocessor preprocessor = null;
    private Postprocessor postprocessor = null;
    private ContextAnalyzer contextAnalyzer = null;

    public Engine(EngineConfig config, int threads) {
        this.config = config;
        this.threads = threads;
        this.root = new File(Config.fs.engines, config.getName());
        this.decoderIniTemplatePath = new File(this.root, MOSES_INI_PATH);
        this.caIndexPath = new File(this.root, CONTEXT_ANALYZER_INDEX_PATH);
        this.configFile = new File(this.root, ENGINE_CONFIG_PATH);
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Decoder getDecoder() {
        if (decoder == null) {
            synchronized (this) {
                if (decoder == null) {
                    try {
                        MosesINI mosesINI = MosesINI.load(decoderIniTemplatePath, root);
                        Map<String, float[]> weights = config.getDecoderConfig().getWeights();

                        if (weights != null)
                            mosesINI.setWeights(weights);

                        mosesINI.setThreads(threads);

                        File inifile = new File(workingDirectory, "moses.ini");
                        FileUtils.write(inifile, mosesINI.toString(), false);
                        decoder = new MosesDecoder(inifile);
                    } catch (IOException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return decoder;
    }

    public Aligner getAligner() {
        if (config.getAlignerConfig().isEnabled() && aligner == null) {
            synchronized (this) {
                if (aligner == null) {
                    aligner = new SymmetrizedAligner(root.getAbsolutePath());

                    try {
                        aligner.init();
                    } catch (IOException | ParseException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return aligner;
    }

    public Preprocessor getPreprocessor() {
        if (preprocessor == null) {
            synchronized (this) {
                if (preprocessor == null) {
                    preprocessor = new Preprocessor(config.getSourceLanguage());
                }
            }
        }

        return preprocessor;
    }

    public Postprocessor getPostprocessor() {
        if (postprocessor == null) {
            synchronized (this) {
                if (postprocessor == null) {
                    postprocessor = new Postprocessor(config.getTargetLanguage());
                }
            }
        }

        return postprocessor;
    }

    public ContextAnalyzer getContextAnalyzer() {
        if (contextAnalyzer == null) {
            synchronized (this) {
                if (contextAnalyzer == null) {
                    try {
                        this.contextAnalyzer = new ContextAnalyzer(caIndexPath);
                    } catch (IOException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return contextAnalyzer;
    }

}
