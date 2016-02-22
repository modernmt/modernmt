package eu.modernmt.processing;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.detokenizer.Detokenizers;
import eu.modernmt.processing.framework.*;
import eu.modernmt.processing.tags.TagMapper;
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

    public Postprocessor(Locale language) {
        pipelineWithDetokenization = new ProcessingPipeline.Builder<Translation, Translation>()
                .add(Detokenizers.forLanguage(language))
                .add(new TagMapper())
                .create();

        pipelineWithoutDetokenization = new ProcessingPipeline.Builder<Translation, Translation>()
                .add(new TagMapper())
                .create();
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
