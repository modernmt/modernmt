package eu.modernmt.model;

public class SentenceUtils {

    /**
     * Check if there is at least one UTF-8 letter or digit in the sentence
     *
     * @param sentence the sentence
     * @return true if at least one word has at least one letter or digit
     */
    public static boolean hasAnyLetterOrDigit(Sentence sentence) {
        for (Word word : sentence.getWords()) {
            if (hasAnyLetterOrDigit(word))
                return true;
        }
        return false;
    }

    /**
     * Check if there is at least one UTF-8 letter or digit in the word
     *
     * @param word the word
     * @return true if word has at least one letter or digit
     */
    public static boolean hasAnyLetterOrDigit(Word word) {
        return word.toString(false)
                .codePoints()
                .anyMatch(Character::isLetterOrDigit);
    }

    public static Translation verbatimTranslation(Sentence sentence) {
        Word[] words = sentence.getWords();
        int length = words.length;

        int[] alignmentPositions = new int[length];
        Word[] translationWords = new Word[length];
        for (int i = 0; i < length; i++) {
            alignmentPositions[i] = i;
            translationWords[i] = new Word(words[i].toString(false), " ", " ");
        }
        Alignment alignment = new Alignment(alignmentPositions, alignmentPositions);

        return new Translation(translationWords, sentence, alignment);
    }

}
