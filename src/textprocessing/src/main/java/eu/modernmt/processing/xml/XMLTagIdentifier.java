package eu.modernmt.processing.xml;

import eu.modernmt.model.Tag;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

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
     * its internal state has been updated by the queue of the call() method
     */
    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) {

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
