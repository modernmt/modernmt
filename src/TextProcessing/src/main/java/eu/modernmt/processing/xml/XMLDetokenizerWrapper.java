package eu.modernmt.processing.xml;

import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.detokenizer.Detokenizer;
import eu.modernmt.processing.framework.ProcessingException;

import java.io.IOException;
import java.util.regex.Matcher;

/**
 * Created by davide on 09/03/16.
 */
public class XMLDetokenizerWrapper implements Detokenizer {

    private Detokenizer detokenizer;

    public XMLDetokenizerWrapper(Detokenizer detokenizer) {
        this.detokenizer = detokenizer;
    }

    @Override
    public Translation call(Translation translation) throws ProcessingException {
        // Unescape XML entities
        Token[] words = translation.getWords();
        String[] texts = new String[words.length];

        StringBuilder text = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            Token word = words[i];

            text.setLength(0);
            text.append(word.getText());

            boolean changed = false;

            Matcher m = XMLStringParser.EntityPattern.matcher(text);

            while (m.find()) {
                int start = m.start();
                int end = m.end();

                Character c = XMLCharacterEntity.unescape(m.group());

                if (c != null) {
                    text.replace(start, end, Character.toString(c));
                    changed = true;
                }
            }

            if (changed) {
                texts[i] = word.getText();
                word.setText(text.toString());
            }
        }

        // Call detokenizer
        detokenizer.call(translation);

        // Restore escaped text
        for (int i = 0; i < words.length; i++) {
            String original = texts[i];
            if (original != null)
                words[i].setText(original);
        }

        return translation;
    }

    @Override
    public void close() throws IOException {
        detokenizer.close();
    }

}
