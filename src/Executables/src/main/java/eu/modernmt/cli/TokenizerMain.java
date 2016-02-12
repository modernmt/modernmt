package eu.modernmt.cli;

import eu.modernmt.processing.Languages;
import eu.modernmt.processing.detokenizer.moses.MosesDetokenizer;
import eu.modernmt.processing.framework.*;
import eu.modernmt.processing.tokenizer.Tokenizers;
import eu.modernmt.processing.util.Splitter;
import eu.modernmt.processing.util.StringJoiner;
import org.apache.commons.io.IOUtils;

import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class TokenizerMain {

    public static void main(String[] args) throws InterruptedException, ProcessingException {
        Locale language = Languages.getSupportedLanguage(args[0]);

        if (language == null)
            throw new IllegalArgumentException("Unsupported language: " + args[0]);

        String languageTag = language.toLanguageTag().substring(0, 2);

        ProcessingPipeline<String, String> pipeline = new ProcessingPipeline.Builder<String, String>()
                .add(new Splitter())
                .add(new MosesDetokenizer(languageTag))
                .add(Tokenizers.forLanguage(language))
                .add(new StringJoiner())
                .create();

        try {
            ProcessingJob<String, String> job = pipeline.createJob(PipelineInputStream.fromInputStream(System.in), PipelineOutputStream.fromOutputStream(System.out));
            job.start();
            job.join();
        } finally {
            IOUtils.closeQuietly(pipeline);
        }
    }

}
