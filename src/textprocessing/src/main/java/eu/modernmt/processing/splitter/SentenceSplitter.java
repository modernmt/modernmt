package eu.modernmt.processing.splitter;

import eu.modernmt.lang.Languages;
import eu.modernmt.model.Word;

import java.util.*;

/**
 * A SentenceSplitter is a preprocessing component that is capable of
 *   - receiving a list of tokens obtained from a text
 *   - decide whether they form multiple sentences
 *   - return the positions after which a sentence ends (splits)
 *
 * E.g.:
 *   base text: "Mr. Smith goes to work. He works as vice president jr. at a big co.op, Why?Because s.r.l.. "Oh!" he says "the weather today is warm".
 *   tokens: [Mr., Smith, goes, to, work, ., He, ...]
 *   SentenceSplitter output: [6,
 *
 */
public abstract class SentenceSplitter {

    private static final SentenceSplitter DEFAULT_IMPL = new DefaultSentenceSplitter();
    private static final Map<Locale, SentenceSplitter> IMPLEMENTATIONS = new HashMap<>();

    static {
        IMPLEMENTATIONS.put(Languages.ENGLISH, DEFAULT_IMPL);
        IMPLEMENTATIONS.put(Languages.FRENCH, DEFAULT_IMPL);
        IMPLEMENTATIONS.put(Languages.ARABIC, DEFAULT_IMPL);
        IMPLEMENTATIONS.put(Languages.SPANISH, DEFAULT_IMPL);
    }

    public static SentenceSplitter forLanguage(Locale locale) {
        return IMPLEMENTATIONS.getOrDefault(locale, DEFAULT_IMPL);
    }

    /**
     * A SplitterWordIterator is a protected class that offers methods to iterate over a Token list
     * in groups of three tokens at a time. This allows to check if the central token is a split.
     *
     * At its creation, the SplitterWordIterator does not point to a valid position.
     * To position it to the first acceptable group of prev-cur-next words, the client needs to call its next() method.
     * */
    protected class SplitterWordIterator implements Iterator<Word> {

        private final Word[] words;

        protected SplitterWordIterator(Word[] words) {
            this.words = words;
        }

        int cur = 0;    // current represents is the position of the current Word (the central one in the group)
                            // It is initialized at 0, which is a NOT ACCEPTABLE POSITION since it has no prev.

        /**
         * Check if there are still groups of three Words that have not been iterated over.
         * In other words, check if after the successor of the current Word there are still available positions in the words array.
         * @return TRUE if there are still Words to scan, FALSE otherwise.
         */
        public boolean hasNext() {
            return cur + 2 < words.length;    //the position after the successor is cur + 2: is it still in the array?
        }

        /**
         * Move the current index to the next element and return that element.
         * WARNING: This method does not check if the next element exists, so it can throw an IndexOutOfBoundsException.
         *          It is highly recommended to use it only after explicitly checking with the hasNext() method.
         */
        public Word next() throws IndexOutOfBoundsException {
            return words[++cur];
        }

        public Word getCurrent() {
            return words[cur];
        }

        public Word getPredecessor() {
            return words[cur - 1];
        }

        public Word getSuccessor() {
            return words[cur + 1];
        }

        public int getCurrentPosition() {
            return cur;
        }

    }

    /**
     * This method returns the positions of the splits in a text.
     * A split is a Word in correspondence of which a sentence ends.
     * So, if splits are found, it means that multiple sentences are contained in the passed text,
     * (and they should be handled separately by the translation engine.)
     * @param originalWords the test to find the splits of, in the form of a Word array
     * @return a List containing the split positions in the array.
     */
    public abstract List<Integer> split(Word[] originalWords);
    /**
     * This method checks if Word is a split point of the original text,
     * by analyzing it and its neighbors (the predecessor and the successor word).
     * @param prev the predecessor to the word to check
     * @param word the current word to check
     * @param next the successor to the word to check
     * @return TRUE if the word is a split; FALSE otherwise
     */
    protected abstract boolean isSplit(Word prev, Word word, Word next);
}
