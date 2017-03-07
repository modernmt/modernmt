package eu.modernmt.processing.xml;

import eu.modernmt.processing.LanguageNotSupportedException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * /**
 * Created by andrea on 02/03/17.
 * <p>
 * An XMLEntityEscaper is the object that, in the processing pipeline,
 * handles the identification of XML Escape Entities
 * and requests to the StringBuider editor their replacement
 * with the character they stand for.
 */
public class XMLEntityEscaper extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    /**
     * This constructor initializes an XMLEntityEscaper,
     * that doesn't need to remember any information
     * about the source or target language.
     *
     * @param sourceLanguage the language of the input String
     * @param targetLanguage the language the input String must be translated to
     * @throws LanguageNotSupportedException the requested language is not supported by this software
     */
    public XMLEntityEscaper(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    /**
     * This method uses a Matcher and the pattern described by XMLCharEntity
     * to find all XML Escape Entities in the input SentenceBuilder current String.
     * For each Entity found, it then proceeds to
     * ask the XMLCharacterEntity what character char it stands for,
     * and requests the SentenceBuilder editor
     * to set a Replacement in order to substitute its text with char.
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

        /*find all substrings matching XML Escape Entities
        in the SentenceBuilder current String*/
        Matcher m = XMLCharacterEntity.EntityPattern.matcher(builder.toString());

        int stringIndex = 0;
        SentenceBuilder.Editor editor = builder.edit();

        /*for each entity found,
        * ask the editor to replace the entity text
        * with the character it stands for,
        * obtained by asking it to the XMLCharacterEntity*/
        while (m.find()) {
            int start = m.start();
            int end = m.end();

            if (stringIndex < start)
                stringIndex = end;

            String entity = m.group();
            Character c = XMLCharacterEntity.unescape(entity);

            if (c != null) {
                editor.replace(start, end - start, c.toString());
            }
        }
        editor.commit();
        return builder;
    }
}