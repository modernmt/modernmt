package eu.modernmt.core.facade.error;

import java.util.Locale;

/**
 * Created by davide on 22/04/16.
 */
public class LanguageNotSupportedException extends TranslationException {

    private static final String MESSAGE = "Languages pair not supported: ";

    public LanguageNotSupportedException(Locale language1, Locale language2) {
        super(MESSAGE + language1 + "->" + language2);
    }

}
