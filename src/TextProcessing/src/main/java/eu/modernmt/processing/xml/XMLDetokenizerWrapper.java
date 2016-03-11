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

        StringBuffer text = new StringBuffer();

        for (int i = 0; i < words.length; i++) {
            Token word = words[i];

            Matcher m = XMLStringParser.EntityPattern.matcher(word.getText());
            text.setLength(0);

            while (m.find()) {
                String group = m.group();

                Character c = XMLCharacterEntity.unescape(group);
                String replacement = c == null ? group : Character.toString(c);

                m.appendReplacement(text, replacement);
            }
            m.appendTail(text);

            texts[i] = word.getText();
            word.setText(text.toString());
        }

        // Call detokenizer
        detokenizer.call(translation);

        // Restore escaped text
        for (int i = 0; i < words.length; i++)
            words[i].setText(texts[i]);

        return translation;
    }

    @Override
    public void close() throws IOException {
        detokenizer.close();
    }

}
