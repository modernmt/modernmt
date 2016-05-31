package eu.modernmt.processing.framework.xml;

import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.TextProcessor;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * Created by davide on 31/05/16.
 */
class XMLProcessorFactory implements IProcessorFactory {

    private final String className;

    XMLProcessorFactory(String className) {
        this.className = className;
    }

    @Override
    public <P, R> TextProcessor<P, R> create(Locale sourceLanguage, Locale targetLanguage) throws ProcessingException {
        try {
            return (TextProcessor<P, R>) Class.forName(className)
                    .getConstructor(Locale.class, Locale.class)
                    .newInstance(sourceLanguage, targetLanguage);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new Error("Unable to instantiate class " + className);
        } catch (ClassCastException | ClassNotFoundException e) {
            throw new ProcessingException("Invalid TextProcessor class specified", e);
        }
    }

    @Override
    public boolean accept(Locale sourceLanguage, Locale targetLanguage) {
        return true;
    }

}
