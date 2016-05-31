package eu.modernmt.processing.framework.xml;

import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;

import java.util.Locale;

/**
 * Created by davide on 31/05/16.
 */
interface IProcessorFactory {

    <P, R> TextProcessor<P, R> create(Locale sourceLanguage, Locale targetLanguage) throws ProcessingException;

    boolean accept(Locale sourceLanguage, Locale targetLanguage);

}
