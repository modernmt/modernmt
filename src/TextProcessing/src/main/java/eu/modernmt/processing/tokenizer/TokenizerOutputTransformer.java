package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.framework.string.XMLEditableString;

import java.util.List;

/**
 * Created by davide on 19/02/16.
 */
@Deprecated
public class TokenizerOutputTransformer {

    @Deprecated
    public static XMLEditableString transform(XMLEditableString text, String[] tokens) {
        XMLEditableString.Editor editor = text.getEditor();


        String string = text.toString();
        int length = string.length();

        int stringIndex = 0;
        int lastPosition = 0;

        for (String token : tokens) {
            int tokenPos = string.indexOf(token, stringIndex);
            stringIndex = tokenPos + token.length();

            lastPosition = tokenPos + token.length();
            if (lastPosition <= length)
                setWord(string, editor, tokenPos, lastPosition - tokenPos);
        }

        return editor.commitChanges();
    }

    private static void setWord(String string, XMLEditableString.Editor editor, int startIndex, int length) {
        int end = startIndex + length;
        boolean hasRightSpace = end < string.length() && string.charAt(end) == ' ';

        editor.setWord(startIndex, length, hasRightSpace);
    }

    @Deprecated
    public static XMLEditableString transform(XMLEditableString string, List<String> tokens) {
        return transform(string, tokens.toArray(new String[tokens.size()]));
    }

}
