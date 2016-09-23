package eu.modernmt.engine;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.lucene.LuceneAnalyzer;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.moses.MosesDecoder;
import eu.modernmt.decoder.moses.MosesINI;
import eu.modernmt.engine.config.EngineConfig;
import eu.modernmt.io.Paths;
import eu.modernmt.persistence.Database;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.persistence.sqlite.SQLiteDatabase;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.TextProcessingModels;
import eu.modernmt.vocabulary.Vocabulary;
import eu.modernmt.vocabulary.rocksdb.RocksDBVocabulary;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 19/04/16.
 */
public class Engine implements Closeable {

    static {
        TextProcessingModels.setPath(FileConst.getResourcePath());
    }

    public static final String ENGINE_CONFIG_PATH = "engine.ini";

    public static File getRootPath(String engine) {
        return FileConst.getEngineRoot(engine);
    }

    public static File getConfigFile(String engine) {
        return new File(FileConst.getEngineRoot(engine), ENGINE_CONFIG_PATH);
    }

    private final EngineConfig config;
    private final int threads;
    private final File root;
    private final File runtime;
    private final String name;

    private Decoder decoder = null;
    private Aligner aligner = null;
    private Preprocessor sourcePreprocessor = null;
    private Preprocessor targetPreprocessor = null;
    private Postprocessor postprocessor = null;
    private ContextAnalyzer contextAnalyzer = null;
    private Vocabulary vocabulary = null;
    private Database database = null;

    public Engine(EngineConfig config, int threads) {
        this.config = config;
        this.threads = threads;
        this.name = config.getName();
        this.root = FileConst.getEngineRoot(name);
        this.runtime = FileConst.getEngineRuntime(name);
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
                    try {
                        File iniTemplate = Paths.join(root, "models", "moses.ini");
                        MosesINI mosesINI = MosesINI.load(iniTemplate, root);

                        Map<String, float[]> featureWeights = config.getDecoderConfig().getWeights();
                        if (featureWeights != null)
                            mosesINI.setWeights(featureWeights);

                        mosesINI.setThreads(threads);

                        File inifile = new File(runtime, "moses.ini");
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
        if (aligner == null) {
            synchronized (this) {
                if (aligner == null) {
                    try {
                        File modelDirectory = Paths.join(root, "models", "phrase_tables");
                        aligner = new FastAlign(modelDirectory);
                    } catch (IOException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return aligner;
    }

    public Preprocessor getSourcePreprocessor() {
        if (sourcePreprocessor == null) {
            synchronized (this) {
                if (sourcePreprocessor == null) {
                    try {
                        sourcePreprocessor = new Preprocessor(getSourceLanguage(), getTargetLanguage(), getVocabulary());
                    } catch (IOException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return sourcePreprocessor;
    }

    public Preprocessor getTargetPreprocessor() {
        if (targetPreprocessor == null) {
            synchronized (this) {
                if (targetPreprocessor == null) {
                    try {
                        targetPreprocessor = new Preprocessor(getTargetLanguage(), getSourceLanguage(), getVocabulary());
                    } catch (IOException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return targetPreprocessor;
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
                    try {
                        File indexPath = Paths.join(root, "models", "context");
                        this.contextAnalyzer = new LuceneAnalyzer(indexPath, getSourceLanguage());
                    } catch (IOException e) {
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
                        File model = Paths.join(this.root, "models", "vocabulary");
                        vocabulary = new RocksDBVocabulary(model);
                    } catch (IOException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return vocabulary;
    }

    public Database getDatabase() {
        if (database == null) {
            synchronized (this) {
                if (database == null) {
                    try {
                        //TODO: hardcoded connection string and password
                        File model = Paths.join(this.root, "models", "db", "domains.db");
                        database = new SQLiteDatabase("jdbc:sqlite:" + model, name, "test");
                    } catch (PersistenceException e) {
                        throw new LazyLoadException(e);
                    }
                }
            }
        }

        return database;
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

    @Override
    public void close() {
        IOUtils.closeQuietly(sourcePreprocessor);
        IOUtils.closeQuietly(targetPreprocessor);
        IOUtils.closeQuietly(postprocessor);

        IOUtils.closeQuietly(decoder);
        IOUtils.closeQuietly(aligner);
        IOUtils.closeQuietly(contextAnalyzer);
        IOUtils.closeQuietly(vocabulary);
    }

}
