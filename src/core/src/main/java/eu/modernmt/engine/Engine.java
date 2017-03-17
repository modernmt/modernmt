package eu.modernmt.engine;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.config.DecoderConfig;
import eu.modernmt.config.EngineConfig;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.lucene.LuceneAnalyzer;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.phrasebased.MosesDecoder;
import eu.modernmt.io.Paths;
import eu.modernmt.persistence.Database;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.persistence.cassandra.CassandraDatabase;
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

/**
 * Created by davide on 19/04/16.
 */
public class Engine implements Closeable {

    static {
        initialize();
    }

    public static void initialize() {
        TextProcessingModels.setPath(FileConst.getResourcePath());
    }

    public static final String ENGINE_CONFIG_PATH = "engine.xconf";

    public static File getRootPath(String engine) {
        return FileConst.getEngineRoot(engine);
    }

    public static File getConfigFile(String engine) {
        return new File(FileConst.getEngineRoot(engine), ENGINE_CONFIG_PATH);
    }

    private final File root;
    private final File runtime;
    private final String name;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;

    private final Decoder decoder;
    private final Aligner aligner;
    private final Preprocessor sourcePreprocessor;
    private final Preprocessor targetPreprocessor;
    private final Postprocessor postprocessor;
    private final ContextAnalyzer contextAnalyzer;
    private final Vocabulary vocabulary;

    public static Engine load(EngineConfig config) throws BootstrapException {
        try {
            return new Engine(config);
        } catch (Exception e) {
            throw new BootstrapException(e);
        }
    }

    private Engine(EngineConfig config) throws IOException, PersistenceException {
        this.name = config.getName();
        this.sourceLanguage = config.getSourceLanguage();
        this.targetLanguage = config.getTargetLanguage();

        this.root = FileConst.getEngineRoot(name);
        this.runtime = FileConst.getEngineRuntime(name);

        this.vocabulary = new RocksDBVocabulary(Paths.join(root, "models", "vocabulary"));
        this.sourcePreprocessor = new Preprocessor(sourceLanguage, targetLanguage, vocabulary);
        this.targetPreprocessor = new Preprocessor(targetLanguage, sourceLanguage, vocabulary);
        this.postprocessor = new Postprocessor(sourceLanguage, targetLanguage, vocabulary);
        this.aligner = new FastAlign(Paths.join(root, "models", "align"));
        this.contextAnalyzer = new LuceneAnalyzer(Paths.join(root, "models", "context"), sourceLanguage);

        DecoderConfig decoderConfig = config.getDecoderConfig();
        if (decoderConfig.isEnabled())
            this.decoder = new MosesDecoder(Paths.join(root, "models", "decoder"), aligner, vocabulary,
                    decoderConfig.getThreads());
        else
            this.decoder = null;
    }

    public String getName() {
        return name;
    }

    public Decoder getDecoder() {
        if (decoder == null)
            throw new UnsupportedOperationException("Decoder unavailable");

        return decoder;
    }

    public Aligner getAligner() {
        if (aligner == null)
            throw new UnsupportedOperationException("Aligner unavailable");

        return aligner;
    }

    public ContextAnalyzer getContextAnalyzer() {
        if (contextAnalyzer == null)
            throw new UnsupportedOperationException("Context Analyzer unavailable");

        return contextAnalyzer;
    }

    public Preprocessor getSourcePreprocessor() {
        return sourcePreprocessor;
    }

    public Preprocessor getTargetPreprocessor() {
        return targetPreprocessor;
    }

    public Postprocessor getPostprocessor() {
        return postprocessor;
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public Locale getSourceLanguage() {
        return sourceLanguage;
    }

    public Locale getTargetLanguage() {
        return targetLanguage;
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
