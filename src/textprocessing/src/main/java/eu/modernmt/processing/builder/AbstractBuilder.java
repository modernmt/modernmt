package eu.modernmt.processing.builder;

import eu.modernmt.lang.Language2;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

/**
 * Created by davide on 01/06/16.
 */
abstract class AbstractBuilder {

    public abstract <P, R> TextProcessor<P, R> create(Language2 sourceLanguage, Language2 targetLanguage) throws ProcessingException;

    public boolean accept(Language2 sourceLanguage, Language2 targetLanguage) {
        return true;
    }

}
