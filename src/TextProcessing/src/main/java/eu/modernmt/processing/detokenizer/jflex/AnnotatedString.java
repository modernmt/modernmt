package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;

import java.io.Reader;
import java.io.StringReader;
import java.util.BitSet;

/**
 * Created by davide on 01/02/16.
 */
public class AnnotatedString {

    private String text;
    private BitSet bits;
    private int length;

    public static AnnotatedString fromTranslation(Translation translation) {
        StringBuilder builder = new StringBuilder();

        Token[] words = translation.getWords();

        for (int i = 0; i < words.length; i++) {
            Token word = words[i];
            builder.append(word.getText());

            if (i < words.length - 1)
                builder.append(' ');
        }

        return new AnnotatedString(builder.toString());
    }

    private AnnotatedString(String string) {
        this.length = string.length();
        this.text = string;
        this.bits = new BitSet(this.length);
    }

    public void removeSpace(int position) {
        if (0 < position && position < length)
            bits.set(position);
    }

    public void keepSpace(int position) {
        if (0 < position && position < length)
            bits.clear(position);
    }

    public Reader getReader() {
        return new StringReader(text);
    }

    public void apply(Translation translation) {
        int index = 0;

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
