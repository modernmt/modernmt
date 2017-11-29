package eu.modernmt.processing.splitter;

import eu.modernmt.model.Word;

import java.util.ArrayList;

public class DefaultSentenceSplitter extends SentenceSplitter {
    private static final String[] STOP_WORDS = new String[]{".", "?", "!"};
    private static final int MIN_SENTENCE_SIZE = 2;

    @Override
    public ArrayList<Integer> split(Word[] originalTokens) {
        ArrayList<Integer> splitPositions = new ArrayList<>();

        if (originalTokens.length < MIN_SENTENCE_SIZE)
            return splitPositions;

        SplitterWordIterator iterator = new SplitterWordIterator(originalTokens);
        int lastSplit = -1;             // the position of the last split found
        while (iterator.hasNext()) {
            Word current = iterator.next();
            Word prev = iterator.getPredecessor();
            Word next = iterator.getSuccessor();
            int currentPosition = iterator.getCurrentPosition();

            if (isSplit(prev, current, next) && currentPosition - lastSplit >= MIN_SENTENCE_SIZE) {
                splitPositions.add(currentPosition);
                lastSplit = currentPosition;
            }
        }

        return splitPositions;
    }

    @Override
    public boolean isSplit(Word prev, Word current, Word next) {
        return isStop(current) && !prev.hasRightSpace() && Character.isUpperCase(next.getText().charAt(0));
    }

    private boolean isStop(Word word) {
        for (String stopWord : STOP_WORDS)
            if (word.getText().equals(stopWord) && word.hasRightSpace())
                return true;
        return false;
    }
}