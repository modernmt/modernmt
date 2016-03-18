package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;

import java.io.Reader;
import java.io.StringReader;
import java.util.BitSet;

/**
 * Created by davide on 01/02/16.
 */
public class SpacesAnnotatedString {

    private String text;
    private BitSet bits;
    private int length;

    public static SpacesAnnotatedString fromTranslation(Translation translation) {
        StringBuilder builder = new StringBuilder();
        builder.append(' ');

        for (Token word : translation.getWords()) {
            builder.append(word.getText());
            builder.append(' ');
        }

        return new SpacesAnnotatedString(builder.toString());
    }

    private SpacesAnnotatedString(String string) {
        this.length = string.length();
        this.text = string;
        this.bits = new BitSet(this.length);
    }

    public void removeSpaceRight(int position) {
        while (0 < position && position < length) {
            if (text.charAt(position) == ' ') {
                bits.set(position);
                break;
            }
            position++;
        }
    }

    public void removeSpaceLeft(int position) {
        while (0 < position && position < length) {
            if (text.charAt(position) == ' ') {
                bits.set(position);
                break;
            }
            position--;
        }
    }

    public Reader getReader() {
        return new StringReader(text);
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
        return text;
    }

}
