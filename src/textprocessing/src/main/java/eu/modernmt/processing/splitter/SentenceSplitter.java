package eu.modernmt.processing.splitter;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.model.Token;
import eu.modernmt.model.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
        return split(sentence, false);
    }

    public static List<Sentence> split(Sentence sentence, boolean includeTags) {
        return split(sentence, includeTags, 4, 200);
    }

    public static List<Sentence> split(Sentence sentence, boolean includeTags, int minLength, int maxLength) {
        ArrayList<Sentence> output = new ArrayList<>(count(sentence));

        boolean pendingSentenceBreak = false;
        SentenceSplit split = new SentenceSplit(includeTags);

        for (Token token : sentence) {
            if (pendingSentenceBreak) {
                if ((token instanceof Tag) && !((Tag) token).isOpeningTag()) {
                    split.append(token);
                    continue;
                } else {
                    output.add(split.toSentence());
                    split.clear();
                }
            }

            if (split.wordcount() > maxLength) {
                Sentence longSentence = split.toSentence();
                split.clear();

                int index = getBestSplitIndex(longSentence);
                split(longSentence, index, split, output);
            }

            split.append(token);
            pendingSentenceBreak = token.isSentenceBreak() && split.wordcount() >= minLength;
        }

        if (split.size() > 0)
            output.add(split.toSentence());

        return output;
    }

    private static Pattern PUNCTUATION_WORD = Pattern.compile("[\\s\\p{IsPunctuation}]+", Pattern.UNICODE_CHARACTER_CLASS);

    private static int getBestSplitIndex(Sentence sentence) {
        Word[] words = sentence.getWords();
        int pivot = words.length / 2;

        for (int i = pivot; i >= 0; i--) {
            String word = words[i].getPlaceholder();
            if (PUNCTUATION_WORD.matcher(word).matches())
                return i;

            int right = pivot + (pivot - i);
            if (right < words.length) {
                word = words[right].getPlaceholder();
                if (PUNCTUATION_WORD.matcher(word).matches())
                    return i;
            }

        }

        return pivot;
    }

    private static void split(Sentence sentence, int index, SentenceSplit split, List<Sentence> output) {
        int i = 0;
        for (Token token : sentence) {
            split.append(token);

            if (i == index) {
                output.add(split.toSentence());
                split.clear();
            }

            i++;
        }

        if (split.size() > 0) {
            output.add(split.toSentence());
            split.clear();
        }
    }

}
