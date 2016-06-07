package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Token;
import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.PipelineOutputStream;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.builder.PipelineBuilder;
import eu.modernmt.processing.framework.builder.XMLPipelineBuilder;
import eu.modernmt.processing.framework.concurrent.PipelineExecutor;
import eu.modernmt.processing.tokenizer.Tokenizer;
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

    private static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors();

    private PipelineExecutor<String, Sentence> executor;

    public Preprocessor(Locale sourceLanguage) throws ProcessingException {
        this(sourceLanguage, null, DEFAULT_THREADS, getDefaultBuilder());
    }

    public Preprocessor(Locale sourceLanguage, Locale targetLanguage) throws ProcessingException {
        this(sourceLanguage, targetLanguage, DEFAULT_THREADS, getDefaultBuilder());
    }

    public Preprocessor(Locale sourceLanguage, Locale targetLanguage, PipelineBuilder<String, Sentence> builder) throws ProcessingException {
        this(sourceLanguage, targetLanguage, DEFAULT_THREADS, builder);
    }

    public Preprocessor(Locale sourceLanguage, Locale targetLanguage, int threads) throws ProcessingException {
        this(sourceLanguage, targetLanguage, threads, getDefaultBuilder());
    }

    public Preprocessor(Locale sourceLanguage, Locale targetLanguage, int threads, PipelineBuilder<String, Sentence> builder) throws ProcessingException {
        this.executor = new PipelineExecutor<>(sourceLanguage, targetLanguage, builder, threads);
    }

    public List<Sentence> process(List<String> text, boolean tokenize) throws ProcessingException {
        HashMap<String, Object> metadata = null;

        if (!tokenize) {
            metadata = new HashMap<>();
            metadata.put(Tokenizer.KEY_ENABLE, false);
        }

        return this.executor.process(text, metadata);
    }

    public Sentence[] process(String[] text, boolean tokenize) throws ProcessingException {
        return process(Arrays.asList(text), tokenize).toArray(new Sentence[text.length]);
    }

    public Sentence process(String text, boolean tokenize) throws ProcessingException {
        HashMap<String, Object> metadata = null;

        if (!tokenize) {
            metadata = new HashMap<>();
            metadata.put(Tokenizer.KEY_ENABLE, false);
        }

        return this.executor.process(text, metadata);
    }

    public void process(PipelineInputStream<String> input, PipelineOutputStream<Sentence> output, boolean tokenize) throws ProcessingException {
        HashMap<String, Object> metadata = null;

        if (!tokenize) {
            metadata = new HashMap<>();
            metadata.put(Tokenizer.KEY_ENABLE, false);
        }

        this.executor.process(input, output, metadata);
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

    private static XMLPipelineBuilder<String, Sentence> getDefaultBuilder() throws ProcessingException {
        String xmlPath = Preprocessor.class.getPackage().getName().replace('.', '/');
        xmlPath = xmlPath + "/preprocessor-default.xml";

        InputStream stream = null;

        try {
            stream = Preprocessor.class.getClassLoader().getResourceAsStream(xmlPath);
            if (stream == null)
                throw new Error("Default preprocessor definition not found: " + xmlPath);

            return XMLPipelineBuilder.loadFromXML(stream);
        } catch (IOException e) {
            throw new ProcessingException("Unable to parse default definition: " + xmlPath, e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static void main(String[] args) throws Throwable {
        Preprocessor preprocessor = new Preprocessor(Locale.ENGLISH);

        try {
            Sentence sentence = preprocessor.process("Maria D'Avalos is the most famous:", true);
            System.out.println(sentence);

            for (Token token : sentence)
                System.out.println("\"" + token + "\"");
        } finally {
            preprocessor.close();
        }
    }

}
