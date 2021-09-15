package eu.modernmt.processing.transliteration;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.TextProcessor;

import java.util.Map;

public class AlphabetNormalizer extends TextProcessor<String, String> {

    public AlphabetNormalizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);

        if (!Language.SERBIAN.isEqualOrMoreGenericThan(sourceLanguage))
            throw new UnsupportedLanguageException(sourceLanguage);
    }

    @Override
    public String call(String text, Map<String, Object> metadata) {
        return Serbian.toLatin(text);
    }

}
