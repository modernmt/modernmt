package eu.modernmt.processing.normalizers;

import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.util.Map;

/**
 * Created by davide on 09/06/16.
 */
public class GuillemetsToQuotesProcessor extends TextProcessor<Translation, Translation> {

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        Word[] source = translation.getSource().getWords();
        Word[] target = translation.getWords();

        if (translation.hasAlignment()) {
            for (int[] pair : translation.getWordAlignment()) {
                Word sourceWord = source[pair[0]];
                Word targetWord = target[pair[1]];

                if (targetWord.hasText())
                    continue;

                String sourceText = sourceWord.getText();
                if (sourceText.length() == 1) {
                    char c = sourceText.charAt(0);

                    if (c == '«' || c == '»')
                        targetWord.setText("\"");
                }
            }
        }

        return translation;
    }

}
