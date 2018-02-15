package eu.modernmt.cleaning;

import eu.modernmt.cleaning.filters.*;
import eu.modernmt.cleaning.filters.draft.DraftFilter;
import eu.modernmt.cleaning.filters.lang.LanguageFilter;
import eu.modernmt.cleaning.filters.ngrams.RareNgramFilter;
import eu.modernmt.cleaning.normalizers.ControlCharsStripper;
import eu.modernmt.cleaning.normalizers.XMLStripper;
import eu.modernmt.cleaning.normalizers.chinese.ChineseNormalizer;
import eu.modernmt.model.corpus.MultilingualCorpus;

/**
 * Created by davide on 02/11/17.
 */
public class CorporaCleaning {

    public static class Options {

        public static Options defaultOptions() {
            Options options = new Options();
            options.normalize = true;
            options.filterByPunctuation = true;
            options.filterOddSentences = true;
            options.filterDrafts = true;
            options.filterBySentenceLength = true;
            options.filterNumericSentences = true;
            options.filterVerbatimTranslations = true;
            options.filterByLanguage = true;
            return options;
        }

        public static Options pairDefaultOptions() {
            Options options = new Options();
            options.normalize = true;
            options.filterByPunctuation = true;
            options.filterNumericSentences = true;
            options.filterVerbatimTranslations = true;
            options.filterOddSentences = false;
            options.filterDrafts = false;
            options.filterBySentenceLength = false;
            options.filterByLanguage = false;
            return options;
        }

        public boolean normalize = false;
        public boolean filterByPunctuation = false;
        public boolean filterOddSentences = false;
        public boolean filterDrafts = false;
        public boolean filterBySentenceLength = false;
        public boolean filterNumericSentences = false;
        public boolean filterVerbatimTranslations = false;
        public boolean filterByLanguage = false;

    }

    public static FilteredMultilingualCorpus wrap(MultilingualCorpus corpus) {
        return wrap(corpus, Options.defaultOptions());
    }

    public static FilteredMultilingualCorpus wrap(MultilingualCorpus corpus, Options options) {
        FilterEngine engine = make(options);
        return new FilteredMultilingualCorpus(corpus, engine);
    }

    public static StringPairFilter forStringPairs() {
        return forStringPairs(Options.pairDefaultOptions());
    }

    public static StringPairFilter forStringPairs(Options options) {
        FilterEngine engine = make(options);
        return new StringPairFilter(engine);
    }

    private static FilterEngine make(Options options) {
        FilterEngine.Builder builder = new FilterEngine.Builder();

        if (options.normalize) {
            builder.add(new ControlCharsStripper());
            builder.add(new XMLStripper());
            builder.add(new ChineseNormalizer());
        }

        builder.add(new EmptyLinesFilter());

        if (options.filterByPunctuation)
            builder.add(new PunctuationFilter());
        if (options.filterNumericSentences)
            builder.add(new NumericTextFilter());
        if (options.filterVerbatimTranslations)
            builder.add(new VerbatimTranslationFilter());
        if (options.filterOddSentences)
            builder.add(new RareNgramFilter());
        if (options.filterDrafts)
            builder.add(new DraftFilter());
        if (options.filterBySentenceLength)
            builder.add(new SentenceLengthFilter());
        if (options.filterByLanguage)
            builder.add(new LanguageFilter());

        return builder.build();
    }

}
