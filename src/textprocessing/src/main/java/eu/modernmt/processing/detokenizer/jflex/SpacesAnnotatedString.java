package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;

import java.io.CharArrayReader;
import java.io.Reader;
import java.util.BitSet;

/**
 * Created by davide on 01/02/16.
 */
public class SpacesAnnotatedString {

    private char[] text;
    private BitSet bits;

    public static SpacesAnnotatedString fromSentence(Sentence sentence) {
        StringBuilder builder = new StringBuilder();
        builder.append(' ');

        Word[] words = sentence.getWords();
        for (int i = 0; i < words.length; i++) {
            builder.append(words[i].getPlaceholder());
            if (i == words.length - 1 || words[i].hasRightSpace())
                builder.append(' ');
        }

        char[] buffer = new char[builder.length()];
        builder.getChars(0, builder.length(), buffer, 0);

        return new SpacesAnnotatedString(buffer);
    }

    private SpacesAnnotatedString(char[] text) {
        this.text = text;
        this.bits = new BitSet(text.length);
    }

    public void removeSpaceRight(int position) {
        while (0 < position && position < text.length) {
            if (text[position] == ' ') {
                bits.set(position);
                break;
            }
            position++;
        }
    }

    public void removeSpaceLeft(int position) {
        while (0 < position && position < text.length) {
            if (text[position] == ' ') {
                bits.set(position);
                break;
            }
            position--;
        }
    }

    public void removeAllSpaces(int start, int end) {
        start = start < 0 ? 0 : start;
        end = end > text.length ? text.length : end;

        for (int i = start; i < end; i++) {
            if (text[i] == ' ')
                bits.set(i);
        }
    }

    public Reader getReader() {
        return new CharArrayReader(text);
    }

    public <S extends Sentence> S apply(S sentence, ApplyFunction function) {
        int index = 1; // Skip first whitespace

        Word[] words = sentence.getWords();

        for (int i = 0; i < words.length; i++) {
            Word word = words[i];
            String placeholder = word.getPlaceholder();
            index += placeholder.length();

            if (word.hasRightSpace()) {
                function.apply(word, !(i == words.length - 1 || bits.get(index)));
                index++;
            }
        }

        return sentence;
    }

    @Override
    public String toString() {
        return new String(text);
    }

    public interface ApplyFunction {

        void apply(Word word, boolean hasSpace);

    }
}
