package eu.modernmt.processing.builder;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

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
