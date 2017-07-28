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
import eu.modernmt.model.LanguagePair;
import eu.modernmt.model.UnsupportedLanguageException;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.TextProcessingModels;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

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

    protected final File root;
    protected final File runtime;
    protected final File models;
    protected final File logs;

    protected final String name;
    protected final Set<LanguagePair> languagePairs;

    protected final Aligner aligner;
    protected final HashMap<LanguagePair, Processors> processors;
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

    protected Engine(EngineConfig config) throws BootstrapException {
        this.name = config.getName();
        this.languagePairs = Collections.unmodifiableSet(config.getLanguagePairs());

        if (this.languagePairs.size() > 1 && !isMultilingual())
            throw new BootstrapException("Engine implementation does not support multiple translation directions.");

        this.root = FileConst.getEngineRoot(name);
        this.runtime = FileConst.getEngineRuntime(name);
        this.models = Paths.join(this.root, "models");
        this.logs = Paths.join(this.runtime, "logs");

        this.processors = new HashMap<>();
        for (LanguagePair pair : this.languagePairs) {
            LanguagePair reversed = pair.reversed();

            try {
                this.processors.put(pair, Processors.forLanguagePair(pair));
                this.processors.put(reversed, Processors.forLanguagePair(reversed));
            } catch (IOException e) {
                throw new BootstrapException("Failed to create processors", e);
            }
        }

        try {
            this.aligner = new FastAlign(Paths.join(this.models, "aligner"));
        } catch (IOException e) {
            throw new BootstrapException("Failed to instantiate aligner", e);
        }

        try {
            this.contextAnalyzer = new LuceneAnalyzer(Paths.join(this.models, "context"));
        } catch (IOException e) {
            throw new BootstrapException("Failed to instantiate context analyzer", e);
        }
    }

    protected abstract boolean isMultilingual();

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

    public Preprocessor getPreprocessor(LanguagePair pair) {
        Processors entry = this.processors.get(pair);
        if (entry == null)
            throw new UnsupportedLanguageException(pair);

        return entry.preprocessor;
    }

    public Postprocessor getPostprocessor(LanguagePair pair) {
        Processors entry = this.processors.get(pair);
        if (entry == null)
            throw new UnsupportedLanguageException(pair);

        return entry.postprocessor;
    }

    public Set<LanguagePair> getAvailableLanguagePairs() {
        return this.languagePairs;
    }

    public boolean isLanguagePairSupported(LanguagePair pair) {
        return this.languagePairs.contains(pair);
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
        for (Processors entry : this.processors.values())
            IOUtils.closeQuietly(entry);

        IOUtils.closeQuietly(aligner);
        IOUtils.closeQuietly(contextAnalyzer);
    }

    protected static final class Processors implements Closeable {

        public final Preprocessor preprocessor;
        public final Postprocessor postprocessor;

        public static Processors forLanguagePair(LanguagePair pair) throws IOException {
            return new Processors(new Preprocessor(pair.source, pair.target), new Postprocessor(pair.source, pair.target));
        }

        public Processors(Preprocessor preprocessor, Postprocessor postprocessor) {
            this.preprocessor = preprocessor;
            this.postprocessor = postprocessor;
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(preprocessor);
            IOUtils.closeQuietly(postprocessor);
        }
    }
}
