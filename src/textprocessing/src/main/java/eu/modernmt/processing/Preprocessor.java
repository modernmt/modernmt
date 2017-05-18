package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.processing.builder.XMLPipelineBuilder;
import eu.modernmt.processing.concurrent.PipelineExecutor;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
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

    public Preprocessor(Locale sourceLanguage) throws IOException {
        this(sourceLanguage, null, DEFAULT_THREADS, null);
    }

    public Preprocessor(Locale sourceLanguage, Locale targetLanguage) throws IOException {
        this(sourceLanguage, targetLanguage, DEFAULT_THREADS, null);
    }

    public Preprocessor(Locale sourceLanguage, Locale targetLanguage, int threads, XMLPipelineBuilder<String, Sentence> builder) throws IOException {
        if (builder == null)
            builder = getDefaultBuilder();

        this.executor = new PipelineExecutor<>(sourceLanguage, targetLanguage, builder, threads);
    }

    public Sentence[] process(String[] text) throws ProcessingException {
        return process(Arrays.asList(text)).toArray(new Sentence[text.length]);
    }

    public List<Sentence> process(List<String> text) throws ProcessingException {
        return this.executor.process(text, null);
    }

    public Sentence process(String text) throws ProcessingException {
        return this.executor.process(text, null);
    }

    public void process(PipelineInputStream<String> input, PipelineOutputStream<Sentence> output) throws ProcessingException {
        this.executor.process(input, output, null);
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
