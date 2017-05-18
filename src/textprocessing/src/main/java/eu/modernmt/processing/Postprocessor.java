package eu.modernmt.processing;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.builder.XMLPipelineBuilder;
import eu.modernmt.processing.concurrent.PipelineExecutor;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 19/02/16.
 */
public class Postprocessor implements Closeable {

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();

    private final PipelineExecutor<Translation, Void> executor;

    public Postprocessor(Locale targetLanguage) throws IOException {
        this(null, targetLanguage, DEFAULT_THREADS, null);
    }

    public Postprocessor(Locale sourceLanguage, Locale targetLanguage) throws IOException {
        this(sourceLanguage, targetLanguage, DEFAULT_THREADS, null);
    }

    public Postprocessor(Locale sourceLanguage, Locale targetLanguage, int threads, XMLPipelineBuilder<Translation, Void> builder) throws IOException {
        if (builder == null)
            builder = getDefaultBuilder();

        this.executor = new PipelineExecutor<>(sourceLanguage, targetLanguage, builder, threads);
    }

    @SuppressWarnings("unchecked")
    public void process(List<? extends Translation> translations) throws ProcessingException {
        this.executor.process((Collection<Translation>) translations, null);
    }

    public void process(Translation[] translation) throws ProcessingException {
        this.executor.process(Arrays.asList(translation), null);
    }

    public void process(Translation translation) throws ProcessingException {
        this.executor.process(translation, null);
    }

    public void process(PipelineInputStream<Translation> input) throws ProcessingException {
        this.executor.process(input, null, null);
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
