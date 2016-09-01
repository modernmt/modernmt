package eu.modernmt.facade.exceptions.validation;

import eu.modernmt.facade.exceptions.ValidationException;

import java.util.Locale;

/**
 * Created by davide on 22/04/16.
 */
public class LanguagePairNotSupportedException extends ValidationException {

    public LanguagePairNotSupportedException(Locale source, Locale target) {
        super("Languages pair not supported: " + source.toLanguageTag() + " > " + target.toLanguageTag());
    }

}
