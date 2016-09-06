package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.processing.builder.XMLPipelineBuilder;
import eu.modernmt.processing.concurrent.PipelineExecutor;
import eu.modernmt.processing.vocabulary.VocabularyLookup;
import eu.modernmt.vocabulary.Vocabulary;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 19/02/16.
 */
public class Preprocessor implements Closeable {

    public static ProcessingPipeline<String, Sentence> createPipeline(Locale sourceLanguage) throws ProcessingException, IOException {
        return getDefaultBuilder().newPipeline(sourceLanguage, null);
    }

    public static ProcessingPipeline<String, Sentence> createPipeline(Locale sourceLanguage, Locale targetLanguage) throws ProcessingException, IOException {
        return getDefaultBuilder().newPipeline(sourceLanguage, targetLanguage);
    }

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();

    private final PipelineExecutor<String, Sentence> executor;
    private final Vocabulary vocabulary;

    public Preprocessor(Locale sourceLanguage) throws IOException {
        this(sourceLanguage, null, null, DEFAULT_THREADS, null);
    }

    public Preprocessor(Locale sourceLanguage, Locale targetLanguage) throws IOException {
        this(sourceLanguage, targetLanguage, null, DEFAULT_THREADS, null);
    }

    public Preprocessor(Locale sourceLanguage, Locale targetLanguage, Vocabulary vocabulary) throws IOException {
        this(sourceLanguage, targetLanguage, vocabulary, DEFAULT_THREADS, null);
    }

    public Preprocessor(Locale sourceLanguage, Locale targetLanguage, Vocabulary vocabulary, int threads, XMLPipelineBuilder<String, Sentence> builder) throws IOException {
        if (builder == null)
            builder = getDefaultBuilder();

        this.executor = new PipelineExecutor<>(sourceLanguage, targetLanguage, builder, threads);
        this.vocabulary = vocabulary;
    }

    public List<Sentence> process(List<String> text) throws ProcessingException {
        return process(text, false);
    }

    public Sentence[] process(String[] text) throws ProcessingException {
        return process(text, false);
    }

    public Sentence process(String text) throws ProcessingException {
        return process(text, false);
    }

    public void process(PipelineInputStream<String> input, PipelineOutputStream<Sentence> output) throws ProcessingException {
        process(input, output, false);
    }

    public List<Sentence> process(List<String> text, boolean storeVocabularyWords) throws ProcessingException {
        return this.executor.process(text, getMetadata(storeVocabularyWords));
    }

    public Sentence[] process(String[] text, boolean storeVocabularyWords) throws ProcessingException {
        return process(Arrays.asList(text), storeVocabularyWords).toArray(new Sentence[text.length]);
    }

    public Sentence process(String text, boolean storeVocabularyWords) throws ProcessingException {
        return this.executor.process(text, getMetadata(storeVocabularyWords));
    }

    public void process(PipelineInputStream<String> input, PipelineOutputStream<Sentence> output, boolean storeVocabularyWords) throws ProcessingException {
        this.executor.process(input, output, getMetadata(storeVocabularyWords));
    }

    private HashMap<String, Object> getMetadata(boolean storeWords) {
        HashMap<String, Object> metadata = null;

        if (vocabulary != null) {
            metadata = new HashMap<>();
            metadata.put(VocabularyLookup.KEY_VOCABULARY, vocabulary);

            if (storeWords)
                metadata.put(VocabularyLookup.KEY_VOCABULARY_STORE_WORDS, Boolean.TRUE);
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

    private static XMLPipelineBuilder<String, Sentence> getDefaultBuilder() throws IOException {
        String xmlPath = Preprocessor.class.getPackage().getName().replace('.', '/');
        xmlPath = xmlPath + "/preprocessor-default.xml";

        InputStream stream = null;

        try {
            stream = Preprocessor.class.getClassLoader().getResourceAsStream(xmlPath);
            if (stream == null)
                throw new Error("Default preprocessor definition not found: " + xmlPath);

            return XMLPipelineBuilder.loadFromXML(stream);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

}
