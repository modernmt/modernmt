package eu.modernmt.processing.detokenizer;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.TextProcessor;

/**
 * Created by davide on 26/01/16.
 */
public abstract class Detokenizer extends TextProcessor<Translation, Translation> {

    public Detokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

}
