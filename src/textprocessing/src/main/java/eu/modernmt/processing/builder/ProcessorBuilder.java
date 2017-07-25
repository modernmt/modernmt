package eu.modernmt.processing.builder;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * Created by davide on 31/05/16.
 */
class ProcessorBuilder extends AbstractBuilder {

    private final String className;

    ProcessorBuilder(String className) {
        this.className = className;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P, R> TextProcessor<P, R> create(Locale sourceLanguage, Locale targetLanguage) throws ProcessingException {
        try {
            return (TextProcessor<P, R>) Class.forName(className)
                    .getConstructor(Locale.class, Locale.class)
                    .newInstance(sourceLanguage, targetLanguage);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassCastException | ClassNotFoundException e) {
            throw new ProcessingException("Invalid TextProcessor class specified: " + className, e);
        }
    }

}
