package eu.modernmt.processing.framework.xml;

import eu.modernmt.processing.framework.LanguageNotSupportedException;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;

import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 31/05/16.
 */
class XMLProcessorGroupFactory implements IProcessorFactory {

    private final List<XMLProcessorFactory> factories;

    public XMLProcessorGroupFactory(List<XMLProcessorFactory> factories) {
        this.factories = factories;
    }

    @Override
    public <P, R> TextProcessor<P, R> create(Locale sourceLanguage, Locale targetLanguage) throws ProcessingException {
        for (XMLProcessorFactory factory : factories) {
            if (factory.accept(sourceLanguage, targetLanguage))
                return factory.create(sourceLanguage, targetLanguage);
        }

        throw new LanguageNotSupportedException(sourceLanguage, targetLanguage);
    }

    @Override
    public boolean accept(Locale sourceLanguage, Locale targetLanguage) {
        return true;
    }

}
