package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Word;
import eu.modernmt.processing.numbers.NumericWordFactory;
import eu.modernmt.processing.string.TokenHook;
import eu.modernmt.processing.string.XMLEditableString;
import eu.modernmt.processing.xmessage.XMessageWordFactory;

import java.util.*;

/**
 * Created by davide on 31/03/16.
 */
public class SentenceBuilder extends TextProcessor<XMLEditableString, Sentence> {

    public interface WordFactory {

        boolean match(String text, String placeholder);

        Word build(String text, String placeholder, String rightSpace);

        void setMetadata(Map<String, Object> metadata);

    }

    private static final List<Class<? extends WordFactory>> FACTORIES = Arrays.asList(
            NumericWordFactory.class, XMessageWordFactory.class
    );

    public SentenceBuilder(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    private static WordFactory[] instantiate(List<Class<? extends WordFactory>> factoryList, Map<String, Object> metadata) {
        WordFactory[] instances = new WordFactory[factoryList.size()];

        int i = 0;
        for (Class<? extends WordFactory> cls : factoryList) {
            try {
                WordFactory factory = cls.newInstance();
                factory.setMetadata(metadata);
                instances[i++] = factory;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new Error("Error while creating WordFactory instance: " + cls.getName(), e);
            }
        }

        return instances;
    }

    private static Word createWord(WordFactory[] wordFactories, String text, String placeholder, String rightSpace) {
        Word word = null;

        for (int i = wordFactories.length - 1; i >= 0; i--) {
            WordFactory factory = wordFactories[i];

            if (factory.match(text, placeholder)) {
                word = factory.build(text, placeholder, rightSpace);
                break;
            }
        }

        if (word == null)
            word = new Word(text, placeholder, rightSpace);

        return word;
    }


    @Override
    public Sentence call(XMLEditableString string, Map<String, Object> metadata) throws ProcessingException {
        WordFactory[] wordFactories = instantiate(FACTORIES, metadata);

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

            if (type == TokenHook.TokenType.Word) {
                Word word = createWord(wordFactories, text, placeholder, space);
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

}
