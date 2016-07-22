package eu.modernmt.processing.framework.builder;

import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;

import java.util.Locale;

/**
 * Created by davide on 01/06/16.
 */
abstract class AbstractBuilder {

    public abstract <P, R> TextProcessor<P, R> create(Locale sourceLanguage, Locale targetLanguage) throws ProcessingException;

    public boolean accept(Locale sourceLanguage, Locale targetLanguage) {
        return true;
    }

}
