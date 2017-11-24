package eu.modernmt.engine;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.config.EngineConfig;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.lucene.LuceneAnalyzer;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.engine.impl.NeuralEngine;
import eu.modernmt.engine.impl.PhraseBasedEngine;
import eu.modernmt.io.FileConst;
import eu.modernmt.io.Paths;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.TextProcessingModels;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Created by davide on 19/04/16.
 */
public abstract class Engine implements Closeable, DataListenerProvider {

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

    protected final String name;
    protected final LanguageIndex languages;

    protected final File root;
    protected final File runtime;
    protected final File models;
    protected final File logs;

    protected final Aligner aligner;
    protected final Preprocessor preprocessor;
    protected final Postprocessor postprocessor;
    protected final ContextAnalyzer contextAnalyzer;

    public static Engine load(EngineConfig config) throws BootstrapException {
        EngineConfig.Type type = config.getType();

        if (type == EngineConfig.Type.NEURAL)
            return new NeuralEngine(config);
        else if (type == EngineConfig.Type.PHRASE_BASED)
            return new PhraseBasedEngine(config);
        else
            throw new BootstrapException("Missing engine type (neural|phrase-based)");
    }

    protected Engine(String name, LanguageIndex languages) {
        this.name = name;
        this.languages = languages;

        this.root = null;
        this.runtime = null;
        this.models = null;
        this.logs = null;

        this.aligner = null;
        this.preprocessor = null;
        this.postprocessor = null;
        this.contextAnalyzer = null;
    }

    protected Engine(EngineConfig config) throws BootstrapException {
        this.name = config.getName();
        this.languages = new LanguageIndex(config.getLanguagePairs());

        this.root = FileConst.getEngineRoot(name);
        this.runtime = FileConst.getEngineRuntime(name);
        this.models = Paths.join(this.root, "models");
        this.logs = Paths.join(this.runtime, "logs");

        try {
            this.preprocessor = new Preprocessor();
        } catch (IOException e) {
            throw new BootstrapException("Failed to load pre-processor", e);
        }

        try {
            this.postprocessor = new Postprocessor();
        } catch (IOException e) {
            throw new BootstrapException("Failed to load post-processor", e);
        }

        if (config.getAlignerConfig().isEnabled()) {
            try {
                this.aligner = new FastAlign(Paths.join(this.models, "aligner"));
            } catch (IOException e) {
                throw new BootstrapException("Failed to instantiate aligner", e);
            }
        } else {
            aligner = null;
        }

        try {
            this.contextAnalyzer = new LuceneAnalyzer(this.languages, Paths.join(this.models, "context"));
        } catch (IOException e) {
            throw new BootstrapException("Failed to instantiate context analyzer", e);
        }
    }

    public abstract ContributionOptions getContributionOptions();

    public String getName() {
        return name;
    }

    public abstract Decoder getDecoder();

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

    public Preprocessor getPreprocessor() {
        return preprocessor;
    }

    public Postprocessor getPostprocessor() {
        return postprocessor;
    }

    public LanguageIndex getLanguages() {
        return this.languages;
    }

    public Set<LanguagePair> getAvailableLanguagePairs() {
        return this.languages.getLanguages();
    }

    public File getRootPath() {
        return root;
    }

    public File getModelsPath() {
        return models;
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
    public Collection<DataListener> getDataListeners() {
        ArrayList<DataListener> listeners = new ArrayList<>();
        listeners.add(contextAnalyzer);
        return listeners;
    }

    public File getLogFile(String name) {
        return new File(this.logs, name);
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(preprocessor);
        IOUtils.closeQuietly(postprocessor);
        IOUtils.closeQuietly(aligner);
        IOUtils.closeQuietly(contextAnalyzer);
    }
}
