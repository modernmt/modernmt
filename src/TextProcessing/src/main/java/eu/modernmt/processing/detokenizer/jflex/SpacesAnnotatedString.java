package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;

import java.io.CharArrayReader;
import java.io.Reader;
import java.util.BitSet;

/**
 * Created by davide on 01/02/16.
 */
public class SpacesAnnotatedString {

    private char[] text;
    private BitSet bits;

    public static SpacesAnnotatedString fromTranslation(Translation translation) {
        StringBuilder builder = new StringBuilder();
        builder.append(' ');

        for (Token word : translation.getWords()) {
            builder.append(word.getText());
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

    public void apply(Translation translation) {
        int index = 1; // Skip first whitespace

        for (Token word : translation.getWords()) {
            String text = word.getText();
            index += text.length();

            word.setRightSpace(!bits.get(index));
            index++;
        }
    }

    @Override
    public String toString() {
        return new String(text);
    }

}
