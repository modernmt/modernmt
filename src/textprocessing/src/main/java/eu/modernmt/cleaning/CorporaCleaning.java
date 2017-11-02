package eu.modernmt.cleaning;

import eu.modernmt.cleaning.filters.EmptyLinesFilter;
import eu.modernmt.cleaning.filters.PunctuationFilter;
import eu.modernmt.cleaning.filters.SentenceLengthFilter;
import eu.modernmt.cleaning.filters.draft.DraftFilter;
import eu.modernmt.cleaning.filters.ngrams.RareNgramFilter;
import eu.modernmt.cleaning.normalizers.ControlCharsStripper;
import eu.modernmt.cleaning.normalizers.XMLStripper;
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
            return options;
        }

        public boolean normalize = false;
        public boolean filterByPunctuation = false;
        public boolean filterOddSentences = false;
        public boolean filterDrafts = false;
        public boolean filterBySentenceLength = false;

    }

    public static FilteredMultilingualCorpus wrap(MultilingualCorpus corpus) {
        return wrap(corpus, Options.defaultOptions());
    }

    public static FilteredMultilingualCorpus wrap(MultilingualCorpus corpus, Options options) {
        FilteredMultilingualCorpus filteredCorpus = new FilteredMultilingualCorpus(corpus);

        if (options.normalize) {
            filteredCorpus.addNormalizer(new ControlCharsStripper());
            filteredCorpus.addNormalizer(new XMLStripper());
        }

        filteredCorpus.addFilter(new EmptyLinesFilter());

        if (options.filterByPunctuation)
            filteredCorpus.addFilter(new PunctuationFilter());
        if (options.filterOddSentences)
            filteredCorpus.addFilter(new RareNgramFilter());
        if (options.filterDrafts)
            filteredCorpus.addFilter(new DraftFilter());
        if (options.filterBySentenceLength)
            filteredCorpus.addFilter(new SentenceLengthFilter());

        return filteredCorpus;
    }

}
