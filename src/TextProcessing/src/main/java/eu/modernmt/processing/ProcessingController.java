package eu.modernmt.processing;

import eu.modernmt.processing.detokenizer.Detokenizers;
import eu.modernmt.processing.framework.ProcessingPipeline;
import eu.modernmt.processing.tokenizer.Tokenizers;
import eu.modernmt.processing.util.Splitter;

import java.util.Locale;

/**
 * Created by davide on 27/01/16.
 */
public class ProcessingController {

    public static ProcessingPipeline<String, String[]> getTokenizePipeline(Locale language) {
        return new ProcessingPipeline.Builder<String, String>()
                .add(new Splitter())
                .add(Detokenizers.forLanguage(language))
                .add(Tokenizers.forLanguage(language))
                .create();
    }

    public static ProcessingPipeline<String[], String> getDetokenizePipeline(Locale language) {
        return new ProcessingPipeline.Builder<String[], String[]>()
                .add(Detokenizers.forLanguage(language))
                .create();
    }

}
