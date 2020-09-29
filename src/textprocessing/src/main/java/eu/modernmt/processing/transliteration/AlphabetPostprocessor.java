package eu.modernmt.processing.transliteration;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.TextProcessor;

import java.util.Map;

public class AlphabetPostprocessor extends TextProcessor<Translation, Translation> {

    public AlphabetPostprocessor(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        if (!Language.SERBIAN.isEqualOrMoreGenericThan(targetLanguage))
            throw new UnsupportedLanguageException(targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) {
        Language target = (Language) metadata.get(TextProcessor.TARGET_LANG_KEY);

        if (Language.CYRILLIC_SCRIPT.equals(target.getScript())) {
            for (Word word : translation.getWords())
                word.setText(Serbian.toCyrillic(word.toString()));
        }

        return translation;
    }

}
