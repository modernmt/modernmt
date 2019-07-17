package eu.modernmt.processing.splitter;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;

import java.util.ArrayList;

public class SentenceSplitter {

    public static int DEFAULT_MIN_SENTENCE_LENGTH = 8;

    private static int maxSplits(Sentence sentence) {
        int result = 0;

        for (Word word : sentence.getWords()) {
            if (word.isSentenceBreak())
                result++;
        }

        return 1 + result;
    }

    private final Word[] words;
    private final int minLength;
    private int begin;
    private int end;
    private final ArrayList<Sentence> accumulator;

    public SentenceSplitter(Sentence sentence) {
        this(sentence, DEFAULT_MIN_SENTENCE_LENGTH);
    }

    /**
     * Splits the sentence into sub-sentences following the Sentence-Break annotation.
     * <b>Important</b>: tags are not included in the resulting sub-sentences
     *
     * @param sentence  the sentence to split
     * @param minLength the minimum desired length of a sub-sentence
     */
    public SentenceSplitter(Sentence sentence, int minLength) {
        this.words = sentence.getWords();
        this.begin = 0;
        this.end = -1;
        this.minLength = minLength;

        int maxSize = maxSplits(sentence);
        this.accumulator = maxSize == 1 ? null : new ArrayList<>(maxSize);
    }

    /**
     * @return an array of sentences obtained by splitting the input one, or {@code null} if no split is computed.
     */
    public Sentence[] split() {
        if (accumulator == null)
            return null;

        accumulator.clear();

        for (int i = 0; i < words.length; i++) {
            if (words[i].isSentenceBreak() || i == words.length - 1) {
                if (end >= 0 && getCurrentSplitSize(i) > minLength)
                    accumulator.add(split(end));

                end = i;
            }
        }

        accumulator.add(split(end));

        return accumulator.toArray(new Sentence[0]);
    }

    private int getCurrentSplitSize(int position) {
        return position + 1 - begin;
    }

    private Sentence split(int end) {
        int size = end + 1 - begin;

        Word[] subWords = new Word[size];
        System.arraycopy(words, begin, subWords, 0, size);

        this.begin = end + 1;
        this.end = -1;

        return new Sentence(subWords);
    }

}
