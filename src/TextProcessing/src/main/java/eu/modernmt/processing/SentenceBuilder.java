package eu.modernmt.processing;

import eu.modernmt.model._Sentence;
import eu.modernmt.model._Tag;
import eu.modernmt.model._Token;
import eu.modernmt.model._Word;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.XMLEditableString;
import eu.modernmt.processing.util.WhitespacesNormalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 31/03/16.
 */
class SentenceBuilder implements TextProcessor<XMLEditableString, _Sentence> {

    @Override
    public _Sentence call(XMLEditableString string) {
        char[] reference = string.getOriginalString().toCharArray();

        List<XMLEditableString.TokenHook> hooks = string.compile();
        int size = hooks.size();

        ArrayList<_Word> words = new ArrayList<>(size);
        ArrayList<_Tag> tags = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            XMLEditableString.TokenHook hook = hooks.get(i);

            int start = hook.getStartIndex();
            int length = hook.getLength();
            XMLEditableString.TokenType type = hook.getTokenType();

            String placeholder = hook.getProcessedString();
            String text = new String(reference, start, length);
            String space = getRightSpace(reference, start + length);
            boolean rightSpaceRequired = hook.hasRightSpace();

            if (type == XMLEditableString.TokenType.Word) {
                _Word word = new _Word(text, placeholder, space, rightSpaceRequired);
                words.add(word);
            } else {
                _Tag tag = _Tag.fromText(text, false, space, words.size());
                tags.add(tag);
            }
        }

        _Sentence sentence = new _Sentence(words.toArray(new _Word[words.size()]), tags.toArray(new _Tag[tags.size()]));

        _Token previous = null;
        for (_Token token : sentence) {
            if (previous != null && token instanceof _Tag)
                ((_Tag) token).setLeftSpace(previous.hasRightSpace());

            previous = token;
        }

        return sentence;
    }

    private static String getRightSpace(char[] reference, int start) {
        int end;
        for (end = start; end < reference.length; end++) {
            if (!WhitespacesNormalizer.isWhitespace(reference[end]))
                break;
        }

        return end > start ? new String(reference, start, end - start) : null;
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
