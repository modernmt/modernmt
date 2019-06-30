package eu.modernmt.processing;

import eu.modernmt.lang.Language2;
import eu.modernmt.lang.UnsupportedLanguageException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Created by davide on 26/01/16.
 */
public abstract class TextProcessor<P, R> {

    public static <T extends TextProcessor> T newInstance(Class<T> cls, Language2 sourceLanguage, Language2 targetLanguage) throws ProcessingException {
        boolean defaultConstructor = false;
        Constructor<? extends T> constructor;
        try {
            constructor = cls.getConstructor(Language2.class, Language2.class);
        } catch (NoSuchMethodException e1) {
            try {
                constructor = cls.getConstructor();
                defaultConstructor = true;
            } catch (NoSuchMethodException e) {
                throw new ProcessingException("Invalid TextProcessor '" + cls.getSimpleName() + "', missing or invalid constructor", e);
            }
        }

        try {
            if (defaultConstructor)
                return constructor.newInstance();
            else
                return constructor.newInstance(sourceLanguage, targetLanguage);
        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException e) {
            throw new ProcessingException("Invalid TextProcessor '" + cls.getSimpleName() + "', missing or invalid constructor", e);
        } catch (ExceptionInInitializerError e) {
            throw new ProcessingException("Unexpected error during TextProcessor initialization for class " + cls.getSimpleName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            else
                throw new ProcessingException("Unexpected error during TextProcessor initialization for class " + cls.getSimpleName(), e);
        }
    }

    public TextProcessor() {
        // Language independent TextProcessor
    }

    public TextProcessor(Language2 sourceLanguage, Language2 targetLanguage) throws UnsupportedLanguageException {
        // TextProcessor depends upon source and/or target language
    }

    public abstract R call(P param, Map<String, Object> metadata) throws ProcessingException;

}
