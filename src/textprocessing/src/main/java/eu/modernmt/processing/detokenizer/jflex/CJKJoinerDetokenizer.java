package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.detokenizer.jflex.annotators.CJKJoinerAnnotator;

import java.io.Reader;
import java.util.Locale;

/**
 * Created by davide on 04/08/17.
 */
public class CJKJoinerDetokenizer extends JFlexDetokenizer {

    public CJKJoinerDetokenizer(Locale sourceLanguage, Locale targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    protected JFlexSpaceAnnotator getAnnotator(Locale language) {
        return new CJKJoinerAnnotator((Reader) null);
    }

}
