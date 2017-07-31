package eu.modernmt.processing.string;

import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.util.Locale;
import java.util.Map;

/**
 * Created by andrea on 02/03/17.
 * <p>
 * A sentenceConstructor is the object that, in the processing pipeline,
 * takes care of the creation of the SentenceBuilder object
 * starting from the String to translate.
 * <p>
 * From now on, all processing activities will be performed
 * passing through the SentenceBuilder and namely its editors,
 * keeping track of which transformation were applied.
 * <p>
 * Therefore the usage of the SentenceConstructor marks the
 * passage from the series of transformation that we don't have to remember about
 * to the series of transformation that it's necessary to keep track of
 * in our processing pipeline.
 * <p>
 * In the future, the SentenceConstructor will also implement caching techniques.
 */
public class SentenceConstructor extends TextProcessor<String, SentenceBuilder> {

    private final SentenceBuilder builder = new SentenceBuilder();

    /**
     * This constructor initializes a SentenceConstructor,
     * that doesn't need to remember any information
     * about the source or target language.
     *
     * @param sourceLanguage the language of the input String
     * @param targetLanguage the language the input String must be translated to
     * @throws UnsupportedLanguageException the requested language is not supported by this software
     */
    public SentenceConstructor(Locale sourceLanguage, Locale targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    /**
     * This method asks the SentenceBuilder to generate a Sentence object,
     * starting from the Transformations list
     * filled by the others TextProcessors during the previous processing phases
     *
     * @param string   the string to translate as it appears
     *                 after the previous processing phases
     *                 (that it is not necessary to keep track of)
     * @param metadata additional information on the current pipe
     *                 (not used in this specific operation)
     * @return the SentenceBuilder generated starting from the input string
     * @throws ProcessingException
     */
    @Override
    public SentenceBuilder call(String string, Map<String, Object> metadata) throws ProcessingException {
        return this.builder.initialize(string);
    }
}