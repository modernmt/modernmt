package eu.modernmt.cleaning.filters.lang.cld2;

import eu.modernmt.cleaning.Filter;
import eu.modernmt.io.WordCounter;
import eu.modernmt.lang.Language;
import eu.modernmt.model.corpus.MultilingualCorpus;

/**
 * Created by davide on 27/12/17.
 */
public class CLD2LanguageFilter implements Filter {

    private static final int MIN_WORDS = 8;

    private final CLD2LanguageDetector detector = new CLD2LanguageDetector();
    private final boolean acceptUnknown;

    public CLD2LanguageFilter(boolean acceptUnknown) {
        this.acceptUnknown = acceptUnknown;
    }

    @Override
    public Initializer getInitializer() {
        return null;
    }

    @Override
    public boolean accept(MultilingualCorpus.StringPair pair, int index) {
        if (WordCounter.count(pair.source, pair.language.source) < MIN_WORDS &&
                WordCounter.count(pair.target, pair.language.target) < MIN_WORDS)
            return true;

        Language source = detector.detect(pair.source, !acceptUnknown);
        Language target = detector.detect(pair.target, !acceptUnknown);

        if (source == null || target == null)
            return acceptUnknown;

        return match(source, pair.language.source) && match(target, pair.language.target);
    }

    private static boolean match(Language test, Language ref) {
        return sameLanguage(test, ref) && sameRegion(test, ref);
    }

    private static boolean sameLanguage(Language test, Language ref) {
        return test.getLanguage().equals(ref.getLanguage());
    }

    private static boolean sameRegion(Language test, Language ref) {
        return test.getRegion() == null || ref.getRegion() == null || test.getRegion().equals(ref.getRegion());
    }

    @Override
    public void clear() {
        // Nothing to do
    }

}
