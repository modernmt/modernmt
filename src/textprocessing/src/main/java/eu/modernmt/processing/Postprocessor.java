package eu.modernmt.processing;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.builder.XMLPipelineBuilder;
import eu.modernmt.processing.concurrent.PipelineExecutor;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 19/02/16.
 */
public class Postprocessor implements Closeable {

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();

    private final PipelineExecutor<Translation, Void> executor;

    public Postprocessor() throws IOException {
        this(DEFAULT_THREADS, getDefaultBuilder());
    }

    public Postprocessor(int threads) throws IOException {
        this(threads, getDefaultBuilder());
    }

    public Postprocessor(int threads, XMLPipelineBuilder<Translation, Void> builder) throws IOException {
        this.executor = new PipelineExecutor<>(builder, threads);
    }

    public void process(LanguagePair language, Translation[] batch) throws ProcessingException {
        this.executor.processBatch(language, batch, new Void[batch.length]);
    }

    public void process(LanguagePair language, List<Translation> batch) throws ProcessingException {
        this.executor.processBatch(language, batch.toArray(new Translation[0]), new Void[batch.size()]);
    }

    public void process(LanguagePair language, Translation text) throws ProcessingException {
        this.executor.process(language, text);
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

    private static XMLPipelineBuilder<Translation, Void> getDefaultBuilder() throws IOException {
        String xmlPath = Postprocessor.class.getPackage().getName().replace('.', '/');
        xmlPath = xmlPath + "/postprocessor-default.xml";
        InputStream stream = null;

        try {
            stream = Postprocessor.class.getClassLoader().getResourceAsStream(xmlPath);
            if (stream == null)
                throw new Error("Default postprocessor definition not found: " + xmlPath);

            return XMLPipelineBuilder.loadFromXML(stream);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

}
