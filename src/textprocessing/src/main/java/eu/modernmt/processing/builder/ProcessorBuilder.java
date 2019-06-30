package eu.modernmt.processing.builder;

import eu.modernmt.lang.Language2;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

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
    public <P, R> TextProcessor<P, R> create(Language2 sourceLanguage, Language2 targetLanguage) throws ProcessingException {
        Class<? extends TextProcessor<P, R>> cls;
        try {
            cls = (Class<? extends TextProcessor<P, R>>) Class.forName(className);
        } catch (ClassCastException | ClassNotFoundException e) {
            throw new ProcessingException("Invalid TextProcessor class: " + className, e);
        }

        return TextProcessor.newInstance(cls, sourceLanguage, targetLanguage);
    }

}
