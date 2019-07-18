package eu.modernmt.processing.splitter;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;

import java.util.ArrayList;
import java.util.List;

public class SentenceSplitter {

    private static int count(Sentence sentence) {
        Word[] words = sentence.getWords();

        int result = 0;
        for (int i = 0; i < words.length; i++) {
            if (words[i].isSentenceBreak() || i == words.length - 1)
                result++;
        }

        return result;
    }

    public static List<Sentence> split(Sentence sentence) {
        return split(sentence, 4);
    }

    public static List<Sentence> split(Sentence sentence, int minLength) {
        ArrayList<Sentence> result = new ArrayList<>(count(sentence));
        Word[] words = sentence.getWords();

        int splitBegin = 0;
        int splitLength = 0;

        for (int i = 0; i < words.length; i++) {
            splitLength++;

            if ((words[i].isSentenceBreak() || i == words.length - 1) && splitLength >= minLength) {
                result.add(split(words, splitBegin, splitLength));
                splitBegin = i + 1;
                splitLength = 0;
            }
        }

        if (splitLength > 0)
            result.add(split(words, splitBegin, splitLength));

        return result;
    }

    private static Sentence split(Word[] words, int begin, int size) {
        Word[] subWords = new Word[size];
        System.arraycopy(words, begin, subWords, 0, size);
        return new Sentence(subWords);
    }

}
