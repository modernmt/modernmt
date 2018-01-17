package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.detokenizer.jflex.annotators.CJKJoinerAnnotator;

import java.io.Reader;

/**
 * Created by davide on 04/08/17.
 */
public class CJKJoinerDetokenizer extends JFlexDetokenizer {

    public CJKJoinerDetokenizer(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    protected JFlexSpaceAnnotator getAnnotator(Language language) {
        return new CJKJoinerAnnotator((Reader) null);
    }

}
