package eu.modernmt;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerFactory;
import eu.modernmt.config.EngineConfig;
import eu.modernmt.constants.Const;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextAnalyzerFactory;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFactory;
import eu.modernmt.io.Paths;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.vocabulary.Vocabulary;
import eu.modernmt.vocabulary.rocksdb.RocksDBVocabulary;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by davide on 19/04/16.
 */
public class Engine {

    public static final String ENGINE_CONFIG_PATH = "engine.ini";
    private static final String VOCABULARY_MODEL_PATH = Paths.join("models", "vocabulary");

    public static File getRootPath(String engine) {
        return new File(Const.fs.engines, engine);
    }

    public static File getConfigFile(String engine) {
        File root = new File(Const.fs.engines, engine);
        return new File(root, ENGINE_CONFIG_PATH);
    }

    private final EngineConfig config;
    private final int threads;
    private final File root;
    private final File runtime;
    private final String name;

    private Decoder decoder = null;
    private Aligner aligner = null;
    private Preprocessor preprocessor = null;
    private Postprocessor postprocessor = null;
    private ContextAnalyzer contextAnalyzer = null;
    private Vocabulary vocabulary = null;

    public Engine(EngineConfig config, int threads) {
        this.config = config;
        this.threads = threads;
        this.name = config.getName();
        this.root = new File(Const.fs.engines, config.getName());
        this.runtime = new File(Const.fs.runtime, name);
    }

    public EngineConfig getConfig() {
        return config;
    }

    public String getName() {
        return name;
    }

    public Decoder getDecoder() {
        if (decoder == null) {
            synchronized (this) {
                if (decoder == null) {
                    DecoderFactory factory = DecoderFactory.getInstance();
                    factory.setEnginePath(root);
                    factory.setRuntimePath(runtime);
                    factory.setFeatureWeights(config.getDecoderConfig().getWeights());
                    factory.setDecoderThreads(threads);

                    try {
                        decoder = factory.create();
                    } catch (IOException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return decoder;
    }

    public Aligner getAligner() {
        if (aligner == null) {
            synchronized (this) {
                if (aligner == null) {
                    AlignerFactory factory = AlignerFactory.getInstance();
                    factory.setEnginePath(root);

                    try {
                        aligner = factory.create();
                    } catch (IOException e) {
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
                    try {
                        preprocessor = new Preprocessor(getSourceLanguage(), getTargetLanguage(), getVocabulary());
                    } catch (IOException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return preprocessor;
    }

    public Postprocessor getPostprocessor() {
        if (postprocessor == null) {
            synchronized (this) {
                if (postprocessor == null) {
                    try {
                        postprocessor = new Postprocessor(getSourceLanguage(), getTargetLanguage(), getVocabulary());
                    } catch (IOException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return postprocessor;
    }

    public ContextAnalyzer getContextAnalyzer() {
        if (contextAnalyzer == null) {
            synchronized (this) {
                if (contextAnalyzer == null) {
                    ContextAnalyzerFactory factory = ContextAnalyzerFactory.getInstance();
                    factory.setEnginePath(root);

                    try {
                        this.contextAnalyzer = factory.create();
                    } catch (ContextAnalyzerException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return contextAnalyzer;
    }

    public Vocabulary getVocabulary() {
        if (vocabulary == null) {
            synchronized (this) {
                if (vocabulary == null) {
                    try {
                        File model = new File(this.root, VOCABULARY_MODEL_PATH);
                        vocabulary = new RocksDBVocabulary(model);
                    } catch (IOException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return vocabulary;
    }

    public Locale getSourceLanguage() {
        return config.getSourceLanguage();
    }

    public Locale getTargetLanguage() {
        return config.getTargetLanguage();
    }

    public File getRootPath() {
        return root;
    }

    public File getRuntimeFolder(String folderName, boolean ensure) throws IOException {
        File folder = new File(this.runtime, folderName);

        if (ensure) {
            FileUtils.deleteDirectory(folder);
            FileUtils.forceMkdir(folder);
        }

        return folder;
    }

}
