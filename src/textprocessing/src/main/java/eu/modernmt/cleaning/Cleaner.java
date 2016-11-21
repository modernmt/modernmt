package eu.modernmt.cleaning;

import eu.modernmt.cleaning.filters.draft.DraftFilter;
import eu.modernmt.model.corpus.BilingualCorpus;

/**
 * Created by davide on 17/11/16.
 */
public class Cleaner {

    public static BilingualCorpus wrap(BilingualCorpus corpus) {
        FilteredBilingualCorpus filteredCorpus = new FilteredBilingualCorpus(corpus);
        filteredCorpus.addFilter(new DraftFilter());

        return filteredCorpus;
    }

}
