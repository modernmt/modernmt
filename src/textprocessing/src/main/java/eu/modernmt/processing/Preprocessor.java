package eu.modernmt.processing;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.builder.XMLPipelineBuilder;
import eu.modernmt.processing.concurrent.PipelineExecutor;
import eu.modernmt.processing.string.SentenceCompiler;
import eu.modernmt.processing.xml.format.InputFormat;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 19/02/16.
 */
public class Preprocessor implements Closeable {

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();

    private final int threads;
    private final PipelineExecutor<String, Sentence> executor;

    public Preprocessor() throws IOException {
        this(DEFAULT_THREADS, getDefaultBuilder());
    }

    public Preprocessor(int threads) throws IOException {
        this(threads, getDefaultBuilder());
    }

    public Preprocessor(int threads, XMLPipelineBuilder<String, Sentence> builder) {
        this.executor = new PipelineExecutor<>(builder, threads);
        this.threads = threads;
    }

    public Sentence[] process(LanguageDirection language, String[] batch) throws ProcessingException {
        return process(language, batch, null);
    }

    public List<Sentence> process(LanguageDirection language, List<String> batch) throws ProcessingException {
        return process(language, batch, null);
    }

    public Sentence process(LanguageDirection language, String text) throws ProcessingException {
        return process(language, text, null);
    }

    public Sentence[] process(LanguageDirection language, String[] batch, InputFormat.Type format) throws ProcessingException {
        Map<String, Object> metadata = getMetadata(format);
        return this.executor.processBatch(metadata, language, batch, new Sentence[batch.length]);
    }

    public List<Sentence> process(LanguageDirection language, List<String> batch, InputFormat.Type format) throws ProcessingException {
        Map<String, Object> metadata = getMetadata(format);
        Sentence[] result = this.executor.processBatch(metadata, language, batch.toArray(new String[0]), new Sentence[batch.size()]);
        return Arrays.asList(result);
    }

    public Sentence process(LanguageDirection language, String text, InputFormat.Type format) throws ProcessingException {
        Map<String, Object> metadata = getMetadata(format);
        return this.executor.process(metadata, language, text);
    }

    private static Map<String, Object> getMetadata(InputFormat.Type format) {
        return format == null ? Collections.emptyMap() : Collections.singletonMap(SentenceCompiler.INPUT_FORMAT_TYPE, format);
    }

    public int getThreads() {
        return threads;
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
