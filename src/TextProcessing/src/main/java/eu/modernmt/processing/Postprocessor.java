package eu.modernmt.processing;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.concurrent.PipelineExecutor;
import eu.modernmt.processing.framework.xml.XMLPipelineFactory;
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

    private PipelineExecutor<Translation, Void> executor;

    public Postprocessor(Locale targetLanguage) throws ProcessingException {
        this(null, targetLanguage);
    }

    public Postprocessor(Locale sourceLanguage, Locale targetLanguage) throws ProcessingException {
        this(sourceLanguage, targetLanguage, Runtime.getRuntime().availableProcessors());
    }

    public Postprocessor(Locale sourceLanguage, Locale targetLanguage, int threads) throws ProcessingException {
        String xmlPath = Preprocessor.class.getPackage().getName().replace('.', '/');
        xmlPath = xmlPath + "/postprocessor-default.xml";

        InputStream stream = null;

        try {
            stream = Preprocessor.class.getClassLoader().getResourceAsStream(xmlPath);
            XMLPipelineFactory<Translation, Void> factory = XMLPipelineFactory.loadFromXML(stream);
            this.executor = new PipelineExecutor<>(sourceLanguage, targetLanguage, factory, threads);
        } catch (IOException e) {
            throw new ProcessingException("Unable to final default postprocessor file", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @SuppressWarnings("unchecked")
    public void process(List<? extends Translation> translations) throws ProcessingException {
        this.executor.process((Collection<Translation>) translations);
    }

    public void process(Translation[] translation) throws ProcessingException {
        this.executor.process(Arrays.asList(translation));
    }

    public void process(Translation translation) throws ProcessingException {
        this.executor.process(translation);
    }

    public void process(PipelineInputStream<Translation> input) throws ProcessingException {
        this.executor.process(input, null);
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

}
