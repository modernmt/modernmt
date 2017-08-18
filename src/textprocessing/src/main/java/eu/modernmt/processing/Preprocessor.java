package eu.modernmt.processing;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.builder.XMLPipelineBuilder;
import eu.modernmt.processing.concurrent.PipelineExecutor;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 19/02/16.
 */
public class Preprocessor implements Closeable {

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();

    private final PipelineExecutor<String, Sentence> executor;

    public Preprocessor() throws IOException {
        this(DEFAULT_THREADS, getDefaultBuilder());
    }

    public Preprocessor(int threads) throws IOException {
        this(threads, getDefaultBuilder());
    }

    public Preprocessor(int threads, XMLPipelineBuilder<String, Sentence> builder) throws IOException {
        this.executor = new PipelineExecutor<>(builder, threads);
    }

    public Sentence[] process(LanguagePair language, String[] batch) throws ProcessingException {
        return this.executor.processBatch(language, batch, new Sentence[batch.length]);
    }

    public List<Sentence> process(LanguagePair language, List<String> batch) throws ProcessingException {
        Sentence[] result = this.executor.processBatch(language, batch.toArray(new String[batch.size()]), new Sentence[batch.size()]);
        return Arrays.asList(result);
    }

    public Sentence process(LanguagePair language, String text) throws ProcessingException {
        return this.executor.process(language, text);
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
