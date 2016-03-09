package eu.modernmt.processing;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.detokenizer.Detokenizer;
import eu.modernmt.processing.detokenizer.Detokenizers;
import eu.modernmt.processing.framework.*;
import eu.modernmt.processing.numbers.NumericTokenExtractor;
import eu.modernmt.processing.recaser.Recaser;
import eu.modernmt.processing.xml.TagMapper;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 19/02/16.
 */
public class Postprocessor implements Closeable {

    private final ProcessingPipeline<Translation, Void> pipelineWithDetokenization;
    private final ProcessingPipeline<Translation, Void> pipelineWithoutDetokenization;

    public static ProcessingPipeline<Translation, Void> getPipeline(Locale language, boolean detokenize, int threads) {
        Detokenizer detokenizer = detokenize ? Detokenizers.forLanguage(language) : null;

        return new ProcessingPipeline.Builder<Translation, Translation>()
                .setThreads(threads)
                .add(new NumericTokenExtractor<>())
                .add(new PlaceholderTransformer())
                .add(detokenizer)
                .add(new Recaser())
                .add(new TagMapper())
                .create();
    }

    public Postprocessor(Locale language) {
        this(language, Runtime.getRuntime().availableProcessors());
    }

    public Postprocessor(Locale language, int threads) {
        pipelineWithDetokenization = getPipeline(language, true, threads);
        pipelineWithoutDetokenization = getPipeline(language, false, threads);
    }

    public void process(List<? extends Translation> translations, boolean detokenize) throws ProcessingException {
        BatchTask task = new BatchTask(translations);
        ProcessingPipeline<Translation, Void> pipeline = detokenize ? pipelineWithDetokenization : pipelineWithoutDetokenization;

        try {
            ProcessingJob<Translation, Void> job = pipeline.createJob(task, task);
            job.start();
            job.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public void process(Translation translation, boolean detokenize) throws ProcessingException {
        if (detokenize)
            pipelineWithDetokenization.process(translation);
        else
            pipelineWithoutDetokenization.process(translation);
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(pipelineWithDetokenization);
        IOUtils.closeQuietly(pipelineWithoutDetokenization);
    }

    private static class BatchTask implements PipelineInputStream<Translation>, PipelineOutputStream<Void> {

        private Translation[] source;
        private int readIndex;

        public BatchTask(List<? extends Translation> translations) {
            this.source = translations.toArray(new Translation[translations.size()]);
            this.readIndex = 0;
        }

        @Override
        public Translation read() {
            if (readIndex < source.length)
                return source[readIndex++];
            else
                return null;
        }

        @Override
        public void write(Void value) {
        }

        @Override
        public void close() throws IOException {
        }

    }
}
