package eu.modernmt;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.PlaceholderTransformer;
import eu.modernmt.processing.detokenizer.Detokenizer;
import eu.modernmt.processing.detokenizer.Detokenizers;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.ProcessingPipeline;
import eu.modernmt.processing.numbers.NumericTokenExtractor;
import eu.modernmt.processing.recaser.Recaser;
import eu.modernmt.processing.tags.TagMapper;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.util.Locale;

/**
 * Created by davide on 19/02/16.
 */
public class StrictPostprocessor implements Closeable {

    private final ProcessingPipeline<Translation, Void> pipelineWithDetokenization;
    private final ProcessingPipeline<Translation, Void> pipelineWithoutDetokenization;

    public static ProcessingPipeline<Translation, Void> getPipeline(Locale language, boolean detokenize, int threads) {
        Detokenizer detokenizer = detokenize ? Detokenizers.forLanguage(language) : null;

        return new ProcessingPipeline.Builder<Translation, Translation>()
                .setThreads(threads)
                .add(detokenizer)
                .add(new NumericTokenExtractor<>())
                .add(new PlaceholderTransformer())
                .add(new Recaser())
                .add(new TagMapper())
                .create();
    }

    public StrictPostprocessor(Locale language) {
        this(language, Runtime.getRuntime().availableProcessors());
    }

    public StrictPostprocessor(Locale language, int threads) {
        pipelineWithDetokenization = getPipeline(language, true, threads);
        pipelineWithoutDetokenization = getPipeline(language, false, threads);
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

}
