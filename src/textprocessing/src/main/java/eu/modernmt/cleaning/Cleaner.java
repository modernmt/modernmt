package eu.modernmt.cleaning;

import eu.modernmt.cleaning.filters.draft.DraftFilter;
import eu.modernmt.model.corpus.MultilingualCorpus;

/**
 * Created by davide on 17/11/16.
 */
public class Cleaner {

    public static MultilingualCorpus wrap(MultilingualCorpus corpus) {
        FilteredMultilingualCorpus filteredCorpus = new FilteredMultilingualCorpus(corpus);
        filteredCorpus.addFilter(new DraftFilter());

        return filteredCorpus;
    }

}
