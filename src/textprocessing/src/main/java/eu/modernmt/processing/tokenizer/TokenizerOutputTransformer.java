package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.string.SentenceBuilder;

import java.util.List;

/**
 * Created by davide on 19/02/16.
 * Updated by Andrea on 1/03/2017
 */
@Deprecated
public class TokenizerOutputTransformer {

    /**
     * Method that, starting from an array of strings that represent
     * the tokens obtained by an external tokenizer in their own syntax
     * asks the SentenceBuilder to create Token objects in the syntax of MMT.
     * (e.g: KuromojiTokenizer uses instances of a separate Token class different from ours;
     * other tokenizers might just extract String portions of the sentenceBuilder string)
     * <p>
     * More in detail, the SentenceBuilder (and its Editors) are asked to
     * build new Word token objects.
     * (this method is not called by tag tokenizers)
     *
     * @param builder a SentenceBuilder that holds the input String
     *                and can generate Editors to process it
     * @param tokens  Array of strings, each string representing a token
     *                identified by the external tokenizer
     *                that has called the transform() method
     * @return the SentenceBuilder received as a parameter;
     * its internal state has been updated by the execution of the call() method.
     * @throws ProcessingException
     */
    @Deprecated
    public static SentenceBuilder transform(SentenceBuilder builder, String[] tokens) throws ProcessingException {
        SentenceBuilder.Editor editor = builder.edit();

        String string = builder.toString();
        int length = string.length();

        int stringIndex = 0;

        for (String token : tokens) {
            int tokenPos = string.indexOf(token, stringIndex);

            if (tokenPos < 0)
                throw new ProcessingException("Unable to find token '" + token + "' starting from index " + stringIndex + " in sentence \"" + builder + "\"");

            int tokenLength = token.length();

            stringIndex = tokenPos + tokenLength;
            if (stringIndex <= length)
                editor.setWord(tokenPos, tokenLength, null);
        }

        return editor.commit();
    }

    /**
     * Version of the transform() method in case the tokens are passed as a List of Strings
     * instead of a String array.
     * It just transforms the List into an array and calls the main transform() method.
     *
     * @param builder a SentenceBuilder that holds the input String
     *                and can generate Editors to process it
     * @param tokens  List of strings, each string representing a token
     *                identified by the external tokenizer
     *                that has called the transform() method
     * @return the SentenceBuilder received as a parameter;
     * its internal state has been updated by the execution of the call() method.
     * @throws ProcessingException
     */
    @Deprecated
    public static SentenceBuilder transform(SentenceBuilder builder, List<String> tokens) throws ProcessingException {
        return transform(builder, tokens.toArray(new String[tokens.size()]));
    }

}
