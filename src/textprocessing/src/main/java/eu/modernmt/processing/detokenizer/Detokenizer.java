package eu.modernmt.processing.detokenizer;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.lang.UnsupportedLanguageException;

import java.util.Locale;

/**
 * Created by davide on 26/01/16.
 */
public abstract class Detokenizer extends TextProcessor<Translation, Translation> {

    public Detokenizer(Locale sourceLanguage, Locale targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

}
