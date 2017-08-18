package eu.modernmt.processing.xml;

import eu.modernmt.model.Tag;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Created by andrea on 02/03/17.
 * <p>
 * An XMLTagIdentifier is the object that, in the processing pipeline,
 * handles the identification of XML Tags and requests
 * to the StringBuider editor their replacement witn a single white space.
 */
public class XMLTagIdentifier extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    /**
     * This constructor initializes an XMLTagIdentifier,
     * that doesn't need to remember any information
     * about the source or target language.
     *
     * @param sourceLanguage the language of the input String
     * @param targetLanguage the language the input String must be translated to
     * @throws UnsupportedLanguageException the requested language is not supported by this software
     */
    public XMLTagIdentifier(Locale sourceLanguage, Locale targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    /**
     * This method uses a Matcher to find all XML tags
     * in the current String of the input SentenceBuilder.
     * For each tag found, it requests the SentenceBuilder editor
     * to set a Tag Token and to replace it in the currentString with " "
     *
     * @param builder  a SentenceBuilder that holds the input String
     *                 and can pass to clients an Editor to process it
     * @param metadata additional information on the current pipe
     *                 (not used in this specific operation)
     * @return the SentenceBuilder received as a parameter;
     * its internal state has been updated by the execution of the call() method
     * @throws ProcessingException
     */
    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) throws
            ProcessingException {

        /*find all substrings matching XML tags in the SentenceBuilder current String*/
        Matcher m = Tag.TagRegex.matcher(builder.toString());

        SentenceBuilder.Editor editor = builder.edit();

        /*for each tag found,
        * ask the creation of a separate Tag token
        * and replace the tag text on the StringBuilder currentString with " " */
        while (m.find()) {
            int start = m.start();
            int end = m.end();

            editor.setTag(start, end - start, " ");

        }

        editor.commit();
        return builder;
    }
}
