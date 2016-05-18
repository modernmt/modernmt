package eu.modernmt.rest.framework.errors;

import eu.modernmt.core.facade.error.LanguageNotSupportedException;
import eu.modernmt.rest.framework.Parameters;

/**
 * Created by davide on 22/04/16.
 */
public class ParameterLanguageNotSupportedException extends Parameters.ParameterParsingException {

    public ParameterLanguageNotSupportedException(LanguageNotSupportedException cause) {
        super(cause);
    }

}
