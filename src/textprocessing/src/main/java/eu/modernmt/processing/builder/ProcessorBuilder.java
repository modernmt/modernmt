package eu.modernmt.processing.builder;

import eu.modernmt.RuntimeErrorException;
import eu.modernmt.lang.Language;
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
    public <P, R> TextProcessor<P, R> create(Language sourceLanguage, Language targetLanguage) {
        Class<? extends TextProcessor<P, R>> cls;
        try {
            cls = (Class<? extends TextProcessor<P, R>>) Class.forName(className);
        } catch (ClassCastException | ClassNotFoundException e) {
            throw new RuntimeErrorException("Invalid TextProcessor class: " + className, e);
        }

        return TextProcessor.newInstance(cls, sourceLanguage, targetLanguage);
    }

}
