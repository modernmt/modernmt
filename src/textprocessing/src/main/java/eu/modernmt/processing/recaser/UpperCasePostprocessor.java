package eu.modernmt.processing.recaser;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.TextProcessor;

import java.util.Map;

/**
 * Created by davide on 03/03/16.
 */
public class UpperCasePostprocessor extends TextProcessor<Translation, Translation> {

    public UpperCasePostprocessor(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) {
        if (translation.getSource().hasAnnotation(UpperCasePreprocessor.ANNOTATION)) {
            Word[] words = translation.getWords();

            if (words.length > 0) {
                for (Word word : words)
                    word.setText(word.getText().toUpperCase());
            }
        }

        return translation;
    }

}
