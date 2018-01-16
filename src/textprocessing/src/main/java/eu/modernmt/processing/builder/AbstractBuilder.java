package eu.modernmt.processing.builder;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

/**
 * Created by davide on 01/06/16.
 */
abstract class AbstractBuilder {

    public abstract <P, R> TextProcessor<P, R> create(Language sourceLanguage, Language targetLanguage) throws ProcessingException;

    public boolean accept(Language sourceLanguage, Language targetLanguage) {
        return true;
    }

}
