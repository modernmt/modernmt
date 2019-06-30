package eu.modernmt.processing.detokenizer.jflex;

import eu.modernmt.lang.Language2;
import eu.modernmt.processing.detokenizer.jflex.annotators.CJKJoinerAnnotator;

import java.io.Reader;

/**
 * Created by davide on 04/08/17.
 */
public class CJKJoinerDetokenizer extends JFlexDetokenizer {

    public CJKJoinerDetokenizer(Language2 sourceLanguage, Language2 targetLanguage) {
        super(sourceLanguage, targetLanguage);
    }

    @Override
    protected JFlexSpaceAnnotator getAnnotator(Language2 language) {
        return new CJKJoinerAnnotator((Reader) null);
    }

}
