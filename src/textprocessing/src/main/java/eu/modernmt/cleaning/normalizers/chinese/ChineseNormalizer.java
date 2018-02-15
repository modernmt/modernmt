package eu.modernmt.cleaning.normalizers.chinese;

import eu.modernmt.cleaning.MultilingualCorpusNormalizer;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;

/**
 * Created by davide on 15/02/18.
 */
public class ChineseNormalizer implements MultilingualCorpusNormalizer {

    private final ChineseDetector detector = ChineseDetector.getInstance();

    @Override
    public void normalize(MultilingualCorpus.StringPair pair, int index) {
        Language source = pair.language.source;
        Language target = pair.language.target;

        if ("zh".equals(pair.language.source.getLanguage()))
            source = detector.detect(pair.source);

        if ("zh".equals(pair.language.target.getLanguage()))
            target = detector.detect(pair.target);

        if (!source.equals(pair.language.source) || !target.equals(pair.language.target))
            pair.language = new LanguagePair(source, target);
    }

}
