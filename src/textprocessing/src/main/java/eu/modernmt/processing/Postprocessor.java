package eu.modernmt.processing;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.builder.PipelineBuilder;
import eu.modernmt.processing.builder.XMLPipelineBuilder;
import eu.modernmt.processing.concurrent.PipelineExecutor;
import eu.modernmt.vocabulary.Vocabulary;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 19/02/16.
 */
public class Postprocessor implements Closeable {

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();

    private PipelineExecutor<Translation, Void> executor;
    private Vocabulary vocabulary = null;

    public Postprocessor(Locale targetLanguage) throws ProcessingException {
        this(null, targetLanguage, DEFAULT_THREADS, getDefaultBuilder());
    }

    public Postprocessor(Locale sourceLanguage, Locale targetLanguage) throws ProcessingException {
        this(sourceLanguage, targetLanguage, DEFAULT_THREADS, getDefaultBuilder());
    }

    public Postprocessor(Locale sourceLanguage, Locale targetLanguage, PipelineBuilder<Translation, Void> builder) throws ProcessingException {
        this(sourceLanguage, targetLanguage, DEFAULT_THREADS, builder);
    }

    public Postprocessor(Locale sourceLanguage, Locale targetLanguage, int threads) throws ProcessingException {
        this(sourceLanguage, targetLanguage, threads, getDefaultBuilder());
    }

    public Postprocessor(Locale sourceLanguage, Locale targetLanguage, int threads, PipelineBuilder<Translation, Void> builder) throws ProcessingException {
        this.executor = new PipelineExecutor<>(sourceLanguage, targetLanguage, builder, threads);
    }

    public void setVocabulary(Vocabulary vocabulary) {
        // TODO: remove must be passed in constructor
        this.vocabulary = vocabulary;
    }

    @SuppressWarnings("unchecked")
    public void process(List<? extends Translation> translations) throws ProcessingException {
        this.executor.process((Collection<Translation>) translations, getMetadata());
    }

    public void process(Translation[] translation) throws ProcessingException {
        this.executor.process(Arrays.asList(translation), getMetadata());
    }

    public void process(Translation translation) throws ProcessingException {
        this.executor.process(translation, getMetadata());
    }

    public void process(PipelineInputStream<Translation> input) throws ProcessingException {
        this.executor.process(input, null, getMetadata());
    }

    private HashMap<String, Object> getMetadata() {
        HashMap<String, Object> metadata = null;

        if (vocabulary != null) {
            metadata = new HashMap<>();
            metadata.put(TextProcessor.KEY_VOCABULARY, vocabulary);
        }

        return metadata;
    }

    @Override
    public void close() {
        this.executor.shutdown();

        try {
            if (!this.executor.awaitTermination(1, TimeUnit.SECONDS))
                this.executor.shutdownNow();
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
        }
    }

    private static XMLPipelineBuilder<Translation, Void> getDefaultBuilder() throws ProcessingException {
        String xmlPath = Postprocessor.class.getPackage().getName().replace('.', '/');
        xmlPath = xmlPath + "/postprocessor-default.xml";
        InputStream stream = null;

        try {
            stream = Postprocessor.class.getClassLoader().getResourceAsStream(xmlPath);
            if (stream == null)
                throw new Error("Default postprocessor definition not found: " + xmlPath);

            return XMLPipelineBuilder.loadFromXML(stream);
        } catch (IOException e) {
            throw new ProcessingException("Unable to parse default definition: " + xmlPath, e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

}
