package eu.modernmt.engine;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.config.DecoderConfig;
import eu.modernmt.config.EngineConfig;
import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.lucene.LuceneAnalyzer;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.DataListenerProvider;
import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderException;
import eu.modernmt.decoder.neural.NeuralDecoder;
import eu.modernmt.io.FileConst;
import eu.modernmt.io.Paths;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.processing.Postprocessor;
import eu.modernmt.processing.Preprocessor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Created by davide on 19/04/16.
 */
public class Engine implements Closeable, DataListenerProvider {

    public static final String ENGINE_CONFIG_PATH = "engine.xconf";

    public static File getConfigFile(String engine) {
        return new File(FileConst.getEngineRoot(engine), ENGINE_CONFIG_PATH);
    }

    private final String name;
    private final LanguageIndex languageIndex;

    private final Aligner aligner;
    private final Preprocessor preprocessor;
    private final Postprocessor postprocessor;
    private final ContextAnalyzer contextAnalyzer;
    private final Decoder decoder;

    public static Engine load(EngineConfig config) throws BootstrapException {
        DecoderConfig decoderConfig = config.getDecoderConfig();

        String name = config.getName();
        LanguageIndex languageIndex = config.getLanguageIndex();

        File models = Paths.join(FileConst.getEngineRoot(name), "models");

        Preprocessor preprocessor;
        try {
            preprocessor = new Preprocessor();
        } catch (IOException e) {
            throw new BootstrapException("Failed to load pre-processor", e);
        }

        Postprocessor postprocessor;
        try {
            postprocessor = new Postprocessor();
        } catch (IOException e) {
            throw new BootstrapException("Failed to load post-processor", e);
        }

        Aligner aligner = null;
        if (config.getAlignerConfig().isEnabled()) {
            try {
                aligner = new FastAlign(Paths.join(models, "aligner"));
            } catch (IOException e) {
                throw new BootstrapException("Failed to instantiate aligner", e);
            }
        }

        ContextAnalyzer contextAnalyzer;
        try {
            contextAnalyzer = new LuceneAnalyzer(Paths.join(models, "context"));
        } catch (IOException e) {
            throw new BootstrapException("Failed to instantiate context analyzer", e);
        }

        Decoder decoder = null;
        if (decoderConfig.isEnabled()) {
            try {
                File decoderModel = new File(models, "decoder");
                String decoderClass = decoderConfig.getDecoderClass();

                if (decoderClass == null) {
                    decoder = new NeuralDecoder(decoderModel, decoderConfig);
                } else {
                    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                    Class<?> decoderCls = classLoader.loadClass(decoderClass);
                    Constructor<?> constructor = decoderCls.getConstructor(File.class, DecoderConfig.class);
                    decoder = (Decoder) constructor.newInstance(decoderModel, decoderConfig);
                }
            } catch (ClassNotFoundException e) {
                throw new BootstrapException("Decoder class not found", e);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new BootstrapException("Invalid decoder class specified: missing constructor", e);
            } catch (DecoderException e) {
                throw new BootstrapException("Failed to instantiate decoder", e);
            } catch (InstantiationException e) {
                throw new BootstrapException("Invalid decoder class specified: class is abstract", e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else if (cause instanceof DecoderException) {
                    throw new BootstrapException("Failed to instantiate decoder", cause);
                }
            }
        }

        return new Engine(name, languageIndex, aligner, preprocessor, postprocessor, contextAnalyzer, decoder);
    }

    protected Engine(String name, LanguageIndex languageIndex,
                     Aligner aligner, Preprocessor preprocessor, Postprocessor postprocessor, ContextAnalyzer contextAnalyzer, Decoder decoder) {
        this.name = name;
        this.languageIndex = languageIndex;
        this.aligner = aligner;
        this.preprocessor = preprocessor;
        this.postprocessor = postprocessor;
        this.contextAnalyzer = contextAnalyzer;
        this.decoder = decoder;
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

    public Preprocessor getPreprocessor() {
        return preprocessor;
    }

    public Postprocessor getPostprocessor() {
        return postprocessor;
    }

    public LanguageIndex getLanguageIndex() {
        return this.languageIndex;
    }

    public Set<LanguagePair> getAvailableLanguagePairs() {
        return this.languageIndex.getLanguages();
    }

    public File getRootPath() {
        return FileConst.getEngineRoot(name);
    }

    public File getModelsPath() {
        return Paths.join(getRootPath(), "models");
    }

    public File getRuntimePath() {
        return FileConst.getEngineRuntime(name);
    }

    public File getLogsPath() {
        return Paths.join(getRuntimePath(), "logs");
    }

    public File createRuntimeFolder(String folderName, boolean ensure) throws IOException {
        File folder = new File(getRuntimePath(), folderName);

        if (ensure) {
            FileUtils.deleteDirectory(folder);
            FileUtils.forceMkdir(folder);
        }

        return folder;
    }

    @Override
    public Collection<DataListener> getDataListeners() {
        ArrayList<DataListener> listeners = new ArrayList<>();
        addDataListener(contextAnalyzer, listeners);
        addDataListener(decoder, listeners);

        return listeners;
    }

    private static void addDataListener(Object object, ArrayList<DataListener> listeners) {
        if (object == null)
            return;

        if (object instanceof DataListener)
            listeners.add((DataListener) object);
        if (object instanceof DataListenerProvider)
            listeners.addAll(((DataListenerProvider) object).getDataListeners());
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(decoder);
        IOUtils.closeQuietly(preprocessor);
        IOUtils.closeQuietly(postprocessor);
        IOUtils.closeQuietly(aligner);
        IOUtils.closeQuietly(contextAnalyzer);
    }
}
