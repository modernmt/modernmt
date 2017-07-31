package eu.modernmt.processing.xml;

import eu.modernmt.model.Translation;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.lang.UnsupportedLanguageException;

import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 22/07/16.
 */
public class XMLTagProcessor extends TextProcessor<Translation, Translation> {

    private final XMLTagProjector projector = new XMLTagProjector();

    public XMLTagProcessor(Locale sourceLanguage, Locale targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        return projector.project(translation);
    }

}
