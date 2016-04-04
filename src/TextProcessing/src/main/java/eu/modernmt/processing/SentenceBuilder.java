package eu.modernmt.processing;

import eu.modernmt.model._Sentence;
import eu.modernmt.model._Tag;
import eu.modernmt.model._Token;
import eu.modernmt.model._Word;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.TokenHook;
import eu.modernmt.processing.framework.string.XMLEditableString;
import eu.modernmt.processing.util.WhitespacesNormalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 31/03/16.
 */
public class SentenceBuilder implements TextProcessor<XMLEditableString, _Sentence> {

    public interface WordFactory {

        boolean match(String text, String placeholder);

        _Word build(String text, String placeholder, String rightSpace, boolean rightSpaceRequired);

    }

    private ArrayList<Class<? extends WordFactory>> factoryList = new ArrayList<>();

    public void addWordFactory(Class<? extends WordFactory> factoryClass) {
        factoryList.add(factoryClass);
    }

    private static WordFactory[] instantiate(ArrayList<Class<? extends WordFactory>> factoryList) {
        WordFactory[] instances = new WordFactory[factoryList.size()];

        int i = 0;
        for (Class<? extends WordFactory> cls : factoryList) {
            try {
                instances[i++] = cls.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new Error("Error while creating WordFactory instance: " + cls.getName(), e);
            }
        }

        return instances;
    }

    private static _Word createWord(WordFactory[] wordFactories, String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
        _Word word = null;

        for (int i = wordFactories.length - 1; i >= 0; i--) {
            WordFactory factory = wordFactories[i];

            if (factory.match(text, placeholder)) {
                word = factory.build(text, placeholder, rightSpace, rightSpaceRequired);
                break;
            }
        }

        if (word == null)
            word = new _Word(text, placeholder, rightSpace, rightSpaceRequired);

        return word;
    }

    @Override
    public _Sentence call(XMLEditableString string) {
        WordFactory[] wordFactories = instantiate(this.factoryList);

        char[] reference = string.getOriginalString().toCharArray();

        List<TokenHook> hooks = string.compile();
        int size = hooks.size();

        ArrayList<_Word> words = new ArrayList<>(size);
        ArrayList<_Tag> tags = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            TokenHook hook = hooks.get(i);

            int start = hook.getStartIndex();
            int length = hook.getLength();
            TokenHook.TokenType type = hook.getTokenType();

            String placeholder = hook.getProcessedString();
            String text = new String(reference, start, length);
            String space = getRightSpace(reference, start + length);
            boolean rightSpaceRequired = hook.hasRightSpace();

            if (type == TokenHook.TokenType.Word) {
                _Word word = createWord(wordFactories, text, placeholder, space, rightSpaceRequired);
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
