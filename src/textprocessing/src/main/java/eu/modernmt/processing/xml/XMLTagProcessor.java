package eu.modernmt.processing.xml;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.util.Map;

/**
 * Created by davide on 22/07/16.
 */
public class XMLTagProcessor extends TextProcessor<Translation, Translation> {

    private final XMLTagProjector projector = new XMLTagProjector();

    public XMLTagProcessor(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
        return projector.project(translation);
    }

}
