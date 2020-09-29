package eu.modernmt.processing;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.builder.XMLPipelineBuilder;
import eu.modernmt.processing.concurrent.PipelineExecutor;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 19/02/16.
 */
public class Postprocessor implements Closeable {

    public static class Options {
        public Language source;
        public Language target;

        public Options(Language source, Language target) {
            this.source = source;
            this.target = target;
        }

        private Map<String, Object> toMetadata() {
            Map<String, Object> metadata = new HashMap<>(2);
            metadata.put(TextProcessor.SOURCE_LANG_KEY, source);
            metadata.put(TextProcessor.TARGET_LANG_KEY, target);
            return metadata;
        }
    }

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();

    private final PipelineExecutor<Translation, Void> executor;

    public Postprocessor() throws IOException {
        this(DEFAULT_THREADS, getDefaultBuilder());
    }

    public Postprocessor(int threads) throws IOException {
        this(threads, getDefaultBuilder());
    }

    public Postprocessor(XMLPipelineBuilder<Translation, Void> builder) {
        this(DEFAULT_THREADS, builder);
    }

    public Postprocessor(int threads, XMLPipelineBuilder<Translation, Void> builder) {
        this.executor = new PipelineExecutor<>(builder, threads);
    }

    public void process(LanguageDirection language, Translation[] batch, Options options) throws ProcessingException, InterruptedException {
        this.executor.processBatch(options.toMetadata(), language, batch, new Void[batch.length]);
    }

    public void process(LanguageDirection language, List<Translation> batch, Options options) throws ProcessingException, InterruptedException {
        this.executor.processBatch(options.toMetadata(), language, batch.toArray(new Translation[0]), new Void[batch.size()]);
    }

    public void process(LanguageDirection language, Translation text, Options options) throws ProcessingException {
        this.executor.process(options.toMetadata(), language, text);
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
