package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.detokenizer.jflex.annotators.KnownEntitiesSpaceAnnotator;

import java.io.Reader;

/**
 * Created by davide on 04/08/17.
 */
public class KnownEntitiesDetokenizer extends JFlexDetokenizer {

    public KnownEntitiesDetokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    protected JFlexSpaceAnnotator getAnnotator(Language language) {
        return new KnownEntitiesSpaceAnnotator((Reader) null);
    }

}
