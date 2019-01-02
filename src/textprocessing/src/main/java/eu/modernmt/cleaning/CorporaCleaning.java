package eu.modernmt.cleaning;

import eu.modernmt.cleaning.filters.*;
import eu.modernmt.cleaning.filters.draft.DraftFilter;
import eu.modernmt.cleaning.filters.lang.OptimaizeLanguageFilter;
import eu.modernmt.cleaning.normalizers.ControlCharsStripper;
import eu.modernmt.cleaning.normalizers.DeepXMLEraser;
import eu.modernmt.cleaning.normalizers.StringSpacingNormalizer;
import eu.modernmt.cleaning.normalizers.XMLStripper;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;

/**
 * Created by davide on 02/11/17.
 */
public class CorporaCleaning {

    public static class Options {

        public static Options defaultOptionsForTraining() {
            Options options = new Options();
            options.eraseXml = true;
            options.filterBrokenUTF8 = true;
            options.filterByPunctuation = true;
            options.filterOddSentences = true;
            options.filterDrafts = true;
            options.filterBySentenceLength = true;
            options.filterNumericSentences = true;
            options.filterVerbatimTranslations = true;
            options.filterByLanguage = true;
            return options;
        }

        public static Options defaultOptionsForMemoryImport() {
            Options options = new Options();
            options.eraseXml = false;
            options.filterBrokenUTF8 = true;
            options.filterByPunctuation = true;
            options.filterOddSentences = true;
            options.filterDrafts = true;
            options.filterBySentenceLength = true;
            options.filterNumericSentences = true;
            options.filterVerbatimTranslations = true;
            options.filterByLanguage = true;
            return options;
        }

        public static Options defaultOptionsForStringPairs() {
            Options options = new Options();
            options.eraseXml = false;
            options.filterBrokenUTF8 = true;
            options.filterByPunctuation = true;
            options.filterNumericSentences = true;
            options.filterVerbatimTranslations = true;
            options.filterOddSentences = false;
            options.filterDrafts = false;
            options.filterBySentenceLength = false;
            options.filterByLanguage = false;
            return options;
        }

        public boolean eraseXml = false;
        public boolean filterBrokenUTF8 = false;
        public boolean filterByPunctuation = false;
        public boolean filterOddSentences = false;
        public boolean filterDrafts = false;
        public boolean filterBySentenceLength = false;
        public boolean filterNumericSentences = false;
        public boolean filterVerbatimTranslations = false;
        public boolean filterByLanguage = false;

    }

    public static FilteredMultilingualCorpus wrap(MultilingualCorpus corpus, Options options) {
        ChainedMultilingualCorpusFilter filter = makeMultilingualFilter(options);
        return new FilteredMultilingualCorpus(corpus, filter, filter);
    }

    public static FilteredCorpus wrap(Corpus corpus, Options options) {
        ChainedCorpusFilter filter = makeFilter(options);
        return new FilteredCorpus(corpus, filter, filter);
    }

    public static ChainedCorpusFilter makeFilter(Options options) {
        MonolingualComposer composer = new MonolingualComposer();
        return compose(composer, options).build();
    }

    public static ChainedMultilingualCorpusFilter makeMultilingualFilter(Options options) {
        MultilingualComposer composer = new MultilingualComposer();
        return compose(composer, options).build();
    }

    private static <T extends Composer> T compose(T composer, Options options) {
        // Too long lines could lead Regex crash
        // due to recursive calls (Memory exhausted error)
        composer.add(new TooLongLinesFilter());

        // Normalization --------------------------------------------------------

        if (options.eraseXml)
            composer.add(new DeepXMLEraser());
        else
            composer.add(new XMLStripper());

        composer.add(new ControlCharsStripper());
        composer.add(new StringSpacingNormalizer());

        // Filtering ------------------------------------------------------------

        composer.add(EmptyLinesFilter.class);

        if (options.filterBrokenUTF8)
            composer.add(BrokenUTF8Filter.class);
        if (options.filterByPunctuation)
            composer.add(PunctuationFilter.class);
        if (options.filterNumericSentences)
            composer.add(NumericTextFilter.class);
        if (options.filterVerbatimTranslations)
            composer.addMultilingual(VerbatimTranslationFilter.class);
        if (options.filterOddSentences)
            composer.add(RareNgramFilter.class);
        if (options.filterDrafts)
            composer.addMultilingual(DraftFilter.class);
        if (options.filterBySentenceLength)
            composer.addMultilingual(SentenceLengthFilter.class);
        if (options.filterByLanguage)
            composer.add(OptimaizeLanguageFilter.class);

        return composer;
    }

    private interface Composer {

        void add(CorpusNormalizer normalizer);

        void add(Class<? extends CorpusFilter> clazz);

        void addMultilingual(Class<? extends MultilingualCorpusFilter> clazz);

    }

    private static class MonolingualComposer implements Composer {

        private final ChainedCorpusFilter.Builder builder = new ChainedCorpusFilter.Builder();

        @Override
        public void add(CorpusNormalizer normalizer) {
            builder.add(normalizer);
        }

        @Override
        public void add(Class<? extends CorpusFilter> clazz) {
            try {
                builder.add(clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public void addMultilingual(Class<? extends MultilingualCorpusFilter> clazz) {
            // Ignore
        }

        public ChainedCorpusFilter build() {
            return builder.build();
        }

    }

    private static class MultilingualComposer implements Composer {

        private final ChainedMultilingualCorpusFilter.Builder builder = new ChainedMultilingualCorpusFilter.Builder();

        @Override
        public void add(CorpusNormalizer normalizer) {
            builder.add(normalizer);
        }

        @Override
        public void add(Class<? extends CorpusFilter> clazz) {
            builder.add(new MultilingualCorpusFilterAdapter(clazz));
        }

        @Override
        public void addMultilingual(Class<? extends MultilingualCorpusFilter> clazz) {
            try {
                builder.add(clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public ChainedMultilingualCorpusFilter build() {
            return builder.build();
        }

    }

}
