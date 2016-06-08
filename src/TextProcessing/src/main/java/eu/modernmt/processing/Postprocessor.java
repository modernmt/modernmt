package eu.modernmt.processing;

import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.detokenizer.jflex.JFlexDetokenizer;
import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.builder.PipelineBuilder;
import eu.modernmt.processing.framework.builder.XMLPipelineBuilder;
import eu.modernmt.processing.framework.concurrent.PipelineExecutor;
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

    private PipelineExecutor<Translation, Void> executor;

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

    public static void main(String[] args) throws Throwable {
        JFlexDetokenizer detokenizer = new JFlexDetokenizer(null, Locale.ENGLISH);
        Translation translation = new Translation(new Word[]{new Word("Finland", " "), new Word("'s", " "), new Word("victory", " ")}, null, null);

        detokenizer.call(translation, null);
        System.out.println(translation);

        for (Token token : translation)
            System.out.println("\"" + token + "\"");
    }
}
