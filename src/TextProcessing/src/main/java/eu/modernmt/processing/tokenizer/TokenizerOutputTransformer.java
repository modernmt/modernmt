package eu.modernmt.processing.tokenizer;

import eu.modernmt.processing.framework.string.ProcessedString;
import eu.modernmt.processing.framework.string.StringEditor;

import java.util.List;

/**
 * Created by davide on 19/02/16.
 */
@Deprecated
public class TokenizerOutputTransformer {

    @Deprecated
    public static ProcessedString transform(ProcessedString text, String[] tokens) {
        StringEditor editor = text.getEditor();

        String string = text.toString();
        int length = string.length();

        int stringIndex = 0;
        int lastPosition = 0;

        for (String token : tokens) {
            int tokenPos = string.indexOf(token, stringIndex);
            stringIndex = tokenPos + token.length();

            if (tokenPos != lastPosition && !isEmpty(string, lastPosition, tokenPos))
                editor.setWord(lastPosition, tokenPos - lastPosition);

            lastPosition = tokenPos + token.length();
            if (lastPosition <= length)
                editor.setWord(tokenPos, lastPosition - tokenPos);
        }

        return editor.commitChanges();
    }

    private static boolean isEmpty(String string, int begin, int end) {
        char[] chars = new char[end - begin];
        string.getChars(begin, end, chars, 0);

        for (char c : chars) {
            if (c != ' ')
                return false;
        }

        return true;
    }

    @Deprecated
    public static ProcessedString transform(ProcessedString string, List<String> tokens) {
        return transform(string, tokens.toArray(new String[tokens.size()]));
    }

}
