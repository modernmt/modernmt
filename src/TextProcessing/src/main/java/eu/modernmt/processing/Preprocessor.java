package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.processing.framework.*;
import eu.modernmt.processing.tags.TagHighlighter;
import eu.modernmt.processing.tokenizer.SimpleTokenizer;
import eu.modernmt.processing.tokenizer.Tokenizers;
import eu.modernmt.processing.util.StringNormalizer;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 19/02/16.
 */
public class Preprocessor implements Closeable {

    private final ProcessingPipeline<String, Sentence> pipelineWithTokenization;
    private final ProcessingPipeline<String, Sentence> pipelineWithoutTokenization;

    public Preprocessor(Locale language) {
        pipelineWithTokenization = new ProcessingPipeline.Builder<String, String>()
                .add(new StringNormalizer())
                .add(Tokenizers.forLanguage(language))
                .add(new TagHighlighter())
                .create();

        pipelineWithoutTokenization = new ProcessingPipeline.Builder<String, String>()
                .add(new StringNormalizer())
                .add(new SimpleTokenizer())
                .add(new TagHighlighter())
                .create();
    }

    public Sentence[] process(List<String> text, boolean tokenize) throws ProcessingException {
        return process(text.toArray(new String[text.size()]), tokenize);
    }

    public Sentence[] process(String[] text, boolean tokenize) throws ProcessingException {
        BatchTask task = new BatchTask(text);
        ProcessingPipeline<String, Sentence> pipeline = tokenize ? pipelineWithTokenization : pipelineWithoutTokenization;

        try {
            ProcessingJob<String, Sentence> job = pipeline.createJob(task, task);
            job.start();
            job.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpected exception", e);
        }

        return task.getResult();
    }

    public Sentence process(String text, boolean tokenize) throws ProcessingException {
        if (tokenize)
            return pipelineWithTokenization.process(text);
        else
            return pipelineWithoutTokenization.process(text);
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(pipelineWithTokenization);
        IOUtils.closeQuietly(pipelineWithoutTokenization);
    }

    private static class BatchTask implements PipelineInputStream<String>, PipelineOutputStream<Sentence> {

        private String[] source;
        private Sentence[] result;
        private int readIndex;
        private int writeIndex;

        public BatchTask(String[] source) {
            this.source = source;
            this.result = new Sentence[source.length];
            this.readIndex = 0;
            this.writeIndex = 0;
        }

        @Override
        public String read() {
            if (readIndex < source.length)
                return source[readIndex++];
            else
                return null;
        }

        @Override
        public void write(Sentence value) {
            result[writeIndex++] = value;
        }

        public Sentence[] getResult() {
            return result;
        }

        @Override
        public void close() throws IOException {
        }

    }
}
