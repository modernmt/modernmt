package eu.modernmt.processing;

import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.TextProcessor;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 02/03/16.
 */
public class WordTransformer extends TextProcessor<Translation, Translation> {

    public WordTransformer(Locale sourceLanguage, Locale targetLanguage) throws LanguageNotSupportedException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) {
        Word[] source = translation.getSource().getWords();
        Word[] target = translation.getWords();

        if (translation.hasAlignment()) {
            for (int[] pair : translation.getAlignment()) {
                Word sourceWord = source[pair[0]];
                Word targetWord = target[pair[1]];

                targetWord.applyTransformation(sourceWord);
            }
        }

        for (Word word : target) {
            if (!word.hasText())
                word.applyTransformation(null);
        }

        return translation;
    }

}
