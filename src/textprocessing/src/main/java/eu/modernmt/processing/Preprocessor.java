package eu.modernmt.processing;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.builder.XMLPipelineBuilder;
import eu.modernmt.processing.concurrent.PipelineExecutor;
import eu.modernmt.processing.splitter.SentenceBreakProcessor;
import eu.modernmt.processing.string.SentenceCompiler;
import eu.modernmt.processing.tags.format.InputFormat;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 19/02/16.
 */
public class Preprocessor implements Closeable {

    public static class Options {
        public InputFormat.Type format = null;
        public boolean splitByNewline = false;

        private Map<String, Object> toMetadata() {
            Map<String, Object> metadata = null;
            if (format != null) {
                if (metadata == null) metadata = new HashMap<>(2);
                metadata.put(SentenceCompiler.INPUT_FORMAT_TYPE, format);
            }
            if (splitByNewline) {
                if (metadata == null) metadata = new HashMap<>(2);
                metadata.put(SentenceBreakProcessor.SPLIT_BY_NEWLINE, Boolean.TRUE);
            }

            return metadata == null ? Collections.emptyMap() : metadata;
        }
    }

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();

    private final int threads;
    private final PipelineExecutor<String, Sentence> executor;

    public Preprocessor() throws IOException {
        this(DEFAULT_THREADS, getDefaultBuilder());
    }

    public Preprocessor(int threads) throws IOException {
        this(threads, getDefaultBuilder());
    }

    public Preprocessor(XMLPipelineBuilder<String, Sentence> builder) {
        this(DEFAULT_THREADS, builder);
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

    public Sentence[] process(LanguageDirection language, String[] batch, Options options) throws ProcessingException {
        Map<String, Object> metadata = getMetadata(options);
        return this.executor.processBatch(metadata, language, batch, new Sentence[batch.length]);
    }

    public List<Sentence> process(LanguageDirection language, List<String> batch, Options options) throws ProcessingException {
        Map<String, Object> metadata = getMetadata(options);
        Sentence[] result = this.executor.processBatch(metadata, language, batch.toArray(new String[0]), new Sentence[batch.size()]);
        return Arrays.asList(result);
    }

    public Sentence process(LanguageDirection language, String text, Options options) throws ProcessingException {
        Map<String, Object> metadata = getMetadata(options);
        return this.executor.process(metadata, language, text);
    }

    private static Map<String, Object> getMetadata(Options options) {
        return options == null ? Collections.emptyMap() : options.toMetadata();
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
