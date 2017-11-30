package eu.modernmt.processing.splitter;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;

public class FrenchSentenceSplitter extends SentenceSplitter {
    private static final String[] STOP_WORDS = new String[]{".", "?", "!"};

    /**
     * This method checks if the a Word at a certain index in a Sentence is a split.
     * In this implementation, it checks the current Word, its predecessor and its successor.
     * The first word (with no predecessors) and the last words (with no successors) are never splits.
     * A word is a split if it is a stop word
     * and either ther are no spaces or there is a non breakable space between it and its predecessor
     * and its successor starts with an uppercase char.
     * @param sentence the sentence to check the presence of splits in
     * @param wordIndex the index of the word to check
     * @return TRUE is the current word is a split, FALSE otherwise.
     */
    @Override
    public boolean isSplit(Sentence sentence, int wordIndex) {
        Word[] words = sentence.getWords();

        /*the the first and last words can not be split (they don't even have predecessor or successor)*/
        if (wordIndex == 0 || wordIndex == words.length)
            return false;

        Word current = words[wordIndex];
        Word predecessor = words[wordIndex-1];
        Word successor = words[wordIndex+1];
        return  isStop(current) &&
                (!predecessor.hasRightSpace() || predecessor.getRightSpace().equals("&nbsp;")) &&
                Character.isUpperCase(successor.getText().charAt(0));
    }

    private boolean isStop(Word word) {
        for (String stopWord : STOP_WORDS)
            if (word.getText().equals(stopWord) && word.hasRightSpace())
                return true;
        return false;
    }
}