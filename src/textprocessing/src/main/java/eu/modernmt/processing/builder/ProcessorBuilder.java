package eu.modernmt.processing.builder;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.lang.reflect.InvocationTargetException;

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
    public <P, R> TextProcessor<P, R> create(Language sourceLanguage, Language targetLanguage) throws ProcessingException {
        try {
            return (TextProcessor<P, R>) Class.forName(className)
                    .getConstructor(Language.class, Language.class)
                    .newInstance(sourceLanguage, targetLanguage);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassCastException | ClassNotFoundException e) {
            throw new ProcessingException("Invalid TextProcessor class specified: " + className, e);
        }
    }

}
