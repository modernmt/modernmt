package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Word;
import eu.modernmt.processing.framework.TextProcessor;
import eu.modernmt.processing.framework.string.TokenHook;
import eu.modernmt.processing.framework.string.XMLEditableString;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 31/03/16.
 */
public class SentenceBuilder implements TextProcessor<XMLEditableString, Sentence> {

    public interface WordFactory {

        boolean match(String text, String placeholder);

        Word build(String text, String placeholder, String rightSpace, boolean rightSpaceRequired);

    }

    private ArrayList<Class<? extends WordFactory>> factoryList = new ArrayList<>();

    void addWordFactory(Class<? extends WordFactory> factoryClass) {
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

    private static Word createWord(WordFactory[] wordFactories, String text, String placeholder, String rightSpace, boolean rightSpaceRequired) {
        Word word = null;

        for (int i = wordFactories.length - 1; i >= 0; i--) {
            WordFactory factory = wordFactories[i];

            if (factory.match(text, placeholder)) {
                word = factory.build(text, placeholder, rightSpace, rightSpaceRequired);
                break;
            }
        }

        if (word == null)
            word = new Word(text, placeholder, rightSpace, rightSpaceRequired);

        return word;
    }

    @Override
    public Sentence call(XMLEditableString string) {
        WordFactory[] wordFactories = instantiate(this.factoryList);

        char[] reference = string.getOriginalString().toCharArray();

        List<TokenHook> hooks = string.compile();
        int size = hooks.size();

        ArrayList<Word> words = new ArrayList<>(size);
        ArrayList<Tag> tags = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            TokenHook hook = hooks.get(i);
            TokenHook nextHook = i < size - 1 ? hooks.get(i + 1) : null;

            int start = hook.getStartIndex();
            int length = hook.getLength();
            TokenHook.TokenType type = hook.getTokenType();

            String placeholder = hook.getProcessedString();
            String text = new String(reference, start, length);
            String space = getRightSpace(reference, start + length, nextHook);
            boolean rightSpaceRequired = hook.hasRightSpace();

            if (type == TokenHook.TokenType.Word) {
                Word word = createWord(wordFactories, text, placeholder, space, rightSpaceRequired);
                words.add(word);
            } else {
                Tag tag = Tag.fromText(text, false, space, words.size());
                tags.add(tag);
            }
        }

        Sentence sentence = new Sentence(words.toArray(new Word[words.size()]), tags.toArray(new Tag[tags.size()]));

        Token previous = null;
        for (Token token : sentence) {
            if (previous != null && token instanceof Tag)
                ((Tag) token).setLeftSpace(previous.hasRightSpace());

            previous = token;
        }

        return sentence;
    }

    private static String getRightSpace(char[] reference, int start, TokenHook nextHook) {
        int end = nextHook == null ? reference.length - 1 : nextHook.getStartIndex();
        if (end > reference.length)
            end = reference.length;

        return end > start ? new String(reference, start, end - start) : null;
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
