package eu.modernmt.processing;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;

import java.util.Map;

/**
 * Created by andrea on 06/03/17.
 */
public class WordTextGuessingProcessor extends TextProcessor<Translation, Translation> {


    public WordTextGuessingProcessor(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) {
        Word[] source = translation.getSource().getWords();
        Word[] target = translation.getWords();

        if (translation.hasAlignment()) {
            for (int[] pair : translation.getWordAlignment()) {
                Word sourceWord = source[pair[0]];
                Word targetWord = target[pair[1]];

                guessText(targetWord, sourceWord);
            }
        }

        for (Word word : target) {
            if (!word.hasText())
                guessText(word, null);
        }

        return translation;
    }


    private static void guessText(Word target, Word source) {
        if (!target.hasText()) {
            if (source == null || !source.getPlaceholder().equals(target.getPlaceholder()))
                target.setText(target.getPlaceholder());
            else
                target.setText(source.getText());
        }
    }
}
