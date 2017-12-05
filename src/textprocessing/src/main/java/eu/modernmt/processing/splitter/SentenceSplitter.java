package eu.modernmt.processing.splitter;

import eu.modernmt.lang.Languages;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;

import java.util.*;

/**
 * A SentenceSplitter is a preprocessing component that is capable of
 *   - receiving a list of tokens obtained from a text
 *   - decide whether they form multiple sentences
 *   - return the positions after which a sentence ends (splits)
 */
public abstract class SentenceSplitter {

    private static final SentenceSplitter DEFAULT_IMPL = new DefaultSentenceSplitter();
    private static final Map<Locale, SentenceSplitter> IMPLEMENTATIONS = new HashMap<>();
    private static final int MIN_SENTENCE_SIZE = 20;

    static {
        IMPLEMENTATIONS.put(Languages.ENGLISH, DEFAULT_IMPL);
    }

    public static SentenceSplitter forLanguage(Locale locale) {
        return IMPLEMENTATIONS.getOrDefault(locale, DEFAULT_IMPL);
    }

    /**
     * This method splits a sentence into multiple sentences by looking for splits among its words.
     * A split is a Word in correspondence of which a sentence ends.
     * So, if splits are found, it means that multiple sentences are contained in the passed text,
     * (and they should be handled separately by the translation engine.)
     * @param originalSentence the text to find the splits of, in the form of a Word array
     * @return a List containing the split positions in the array.
     */
    public Sentence[] split(Sentence originalSentence) {
        Word[] originalWords = originalSentence.getWords();
        List<Sentence> splitSentences = new ArrayList<>();

        int prevSplit = -1;
        for (int i = 0; i < originalWords.length; i++) {
            /* i - prevSplit is the amount of words seen since the last split. Do not even check the split if it is too low */
            if (i - prevSplit < MIN_SENTENCE_SIZE)
                continue;

            if (isSplit(originalSentence, i)) {
                Word[] newSplitSentenceWords = new Word[i - prevSplit];
                for (int j = 0; j < i - prevSplit; j++)
                    newSplitSentenceWords[j] = originalWords[j + prevSplit + 1];
                splitSentences.add(new Sentence(newSplitSentenceWords));
                prevSplit = i;
            }
        }

        //handle the last sentence
        Word[] lastSplitSentenceWords = new Word[originalWords.length - 1 - prevSplit];
        for (int j = 0; j < originalWords.length - 1 - prevSplit; j++)
            lastSplitSentenceWords[j] = originalWords[j + prevSplit + 1];
        splitSentences.add(new Sentence(lastSplitSentenceWords));

        Sentence[] array = new Sentence[splitSentences.size()];
        return splitSentences.toArray(array);
    }

    /**
     * This method checks if the a Word at a certain index in a Sentence is a split.
     * @param sentence the sentence to check the presence of splits in
     * @param wordIndex the index of the word to check
     * @return TRUE is the current word is a split, FALSE otherwise.
     */
    protected abstract boolean isSplit(Sentence sentence, int wordIndex);
}
