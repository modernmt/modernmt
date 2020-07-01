package eu.modernmt.processing.normalizers;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.Map;

/**
 * Created by davide on 19/02/16.
 * Updated by andrearossi on 01/03/17
 * <p>
 * A RareCharsNormalizer has the responsibility
 * to know what are the whitespace characters to take into account,
 * to know how to process them (delete them or replace them) case by case
 * to look for whitespace characters sequences in toe string under analysis
 * and to actively request the specific processing they need.
 */
public class WhitespacesNormalizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

    /**
     * Method that, given a SentenceBuilder with the string to process,
     * extracts the string, scans it looking for whitespace characters sequences
     * and requests either their deletion,
     * if they are at the very beginning or at the very end end of the string
     * or their their replacement with a blank space (" ") in any other case.
     *
     * @param builder  a SentenceBuilder that holds the input String
     *                 and can generate Editors to process it
     * @param metadata additional information on the current pipe
     *                 (not used in this specific operation)
     * @return the SentenceBuilder received as a parameter;
     * its internal state has been updated by the queue of the call() method
     */
    @Override
    public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) {
        /*the current version of the String in the SentenceBuilder*/
        char[] source = builder.toCharArray();
        /*an Editor can analyze the text to request transformations*/
        SentenceBuilder.Editor editor = builder.edit();

        boolean sentenceBegin = true;

        /*the position of first whitespace char in the current whitespace chars sequence,
        during the string scan; it is -1 if we are not in a whitespace sequence*/
        int whitespaceStart = -1;

        /*scan all the characters in the string*/
        int i;
        for (i = 0; i < source.length; i++) {
            char c = source[i];
            /*if the current character is a whitespace character*/
            if (isWhitespace(c)) {
                /*if whitespaceStart is still -1,
                 * then this is the first whitespace char in a new whitespace chars sequence
                 * so update whitespaceStart to the current position*/
                if (whitespaceStart == -1)
                    whitespaceStart = i;
                     /*else, this is a whitespace char but it is not the first in the sequence,
                     so do nothing*/

                /*if the current character is not a whitespace character*/
            } else {
                if (whitespaceStart >= 0) {
                    /*if the whitespace sequence is at the beginning of the string
                    the editor must just delete it*/
                    if (sentenceBegin)
                        editor.delete(whitespaceStart, i - whitespaceStart);
                        /*else, it the editor must replace it with a blank space (" ")*/
                    else
                        editor.replace(whitespaceStart, i - whitespaceStart, " ");
                    /*in both cases, mark that we are not in a whitespace sequence anymore*/
                    whitespaceStart = -1;
                }

                sentenceBegin = false;
            }
        }

        /*if the string ends with a whitespace sequence,
         * after the scan is over whitespaceStart will still be >= 0;
         * in this case too these whitespaces must be deleted, not just replaced*/
        if (whitespaceStart >= 0)
            editor.delete(whitespaceStart, i - whitespaceStart);

        return editor.commit();
    }

    /**
     * Method that checks if a character is a whitespace character.
     *
     * @param c the character to check
     * @return a boolean value expressing whether c is a whitespace or not
     */
    public static boolean isWhitespace(char c) {
        return ((0x0009 <= c && c <= 0x000D) || c == '\u0020' || c == '\u00A0' || c == '\u1680' ||
                (0x2000 <= c && c <= 0x200A) || c == '\u2028' || c == '\u202F' || c == '\u205F' || c == '\u3000');
    }
}