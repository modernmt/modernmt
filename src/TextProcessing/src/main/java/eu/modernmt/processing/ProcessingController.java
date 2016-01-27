package eu.modernmt.processing;

import eu.modernmt.processing.detokenizer.moses.MosesDetokenizer;
import eu.modernmt.processing.framework.ProcessingPipeline;
import eu.modernmt.processing.tokenizer.Tokenizers;
import eu.modernmt.processing.util.Splitter;

import java.util.Locale;

/**
 * Created by davide on 27/01/16.
 */
public class ProcessingController {

    public static ProcessingPipeline<String, String[]> getTokenizePipeline(Locale language) {
        String languageTag = language.toLanguageTag().substring(0, 2);

        return new ProcessingPipeline.Builder<String, String>()
                .add(new Splitter())
                .add(new MosesDetokenizer(languageTag))
                .add(Tokenizers.forLanguage(language))
                .create();
    }

    public static ProcessingPipeline<String[], String> getDetokenizePipeline(Locale language) {
        String languageTag = language.toLanguageTag().substring(0, 2);

        return new ProcessingPipeline.Builder<String[], String[]>()
                .add(new MosesDetokenizer(languageTag))
                .create();
    }

}
